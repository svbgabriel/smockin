package com.smockin.admin.ui.views.http;

import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.enums.ApiImportTypeEnum;
import com.smockin.admin.service.ApiImportRouter;
import com.smockin.admin.service.MockedServerEngineService;
import com.smockin.admin.dto.MockImportConfigDTO;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.service.MockDefinitionImportExportService;
import com.smockin.admin.dto.response.RestfulMockResponseDTO;
import com.smockin.admin.service.RestfulMockService;
import com.smockin.admin.ui.layout.MainLayout;
import com.smockin.admin.ui.security.UserSession;
import com.smockin.admin.ui.utils.UIFormattingUtils;
import com.smockin.mockserver.dto.MockServerState;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "http-mocks", layout = MainLayout.class)
@PageTitle("HTTP Mocks | sMockin")
@PermitAll
public class HttpMocksView extends VerticalLayout {

    private final RestfulMockService restfulMockService;
    private final MockedServerEngineService mockedServerEngineService;
    private final MockDefinitionImportExportService importExportService;
    private final ApiImportRouter apiImportRouter;
    private final UserSession userSession;
    private final Logger logger = LoggerFactory.getLogger(HttpMocksView.class);

    private Grid<RestfulMockResponseDTO> grid = new Grid<>(RestfulMockResponseDTO.class);
    private Button startStopButton;
    private Span serverStatus;

    public HttpMocksView(RestfulMockService restfulMockService, MockedServerEngineService mockedServerEngineService, MockDefinitionImportExportService importExportService, ApiImportRouter apiImportRouter, UserSession userSession) {
        this.restfulMockService = restfulMockService;
        this.mockedServerEngineService = mockedServerEngineService;
        this.importExportService = importExportService;
        this.apiImportRouter = apiImportRouter;
        this.userSession = userSession;

        setSizeFull();
        configureGrid();

        add(getToolbar(), grid);
        updateList();
        updateServerStatus();
        
        setupShortcuts();
    }

    private void setupShortcuts() {
        Shortcuts.addShortcutListener(this, () -> addMock(), Key.KEY_N, KeyModifier.ALT);
        Shortcuts.addShortcutListener(this, () -> updateList(), Key.F5);
    }

    private void configureGrid() {
        grid.addClassNames("http-mocks-grid");
        grid.setSizeFull();
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.removeAllColumns();
        grid.addColumn(RestfulMockResponseDTO::getPath).setHeader("Path").setSortable(true);
        grid.addColumn(mock -> UIFormattingUtils.formatMethod(mock.getMethod())).setHeader("Method").setSortable(true);
        grid.addColumn(mock -> UIFormattingUtils.formatStatus(mock.getStatus())).setHeader("Status").setSortable(true);
        grid.addColumn(mock -> UIFormattingUtils.formatMockType(mock.getMockType())).setHeader("Type").setSortable(true);
        grid.addColumn(RestfulMockResponseDTO::getDateCreated).setHeader("Date Created").setSortable(true);
        grid.getColumns().forEach(col -> col.setAutoWidth(true));

        grid.addComponentColumn(mock -> {
            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addClickListener(e -> editMock(mock));

            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e -> deleteMock(mock));

            HorizontalLayout actions = new HorizontalLayout(editButton, deleteButton);
            actions.setSpacing(false);
            return actions;
        }).setHeader("Actions");
    }

    private HorizontalLayout getToolbar() {
        Button addMockButton = new Button("New Mock");
        addMockButton.addClickListener(click -> addMock());

        Button importButton = new Button("Import", VaadinIcon.UPLOAD.create());
        importButton.addClickListener(click -> openImportDialog());

        Anchor exportAnchor = new Anchor();
        exportAnchor.getElement().setAttribute("download", true);
        Button exportButton = new Button("Export", VaadinIcon.DOWNLOAD.create());
        exportAnchor.add(exportButton);
        
        exportAnchor.setHref(new StreamResource("mocks_export.zip", () -> {
            try {
                List<RestfulMockResponseDTO> selected = grid.getSelectedItems().stream().toList();
                List<String> idsToExport;
                
                if (selected.isEmpty()) {
                    idsToExport = restfulMockService.loadAll(userSession.getToken()).stream()
                            .map(RestfulMockResponseDTO::getExtId)
                            .collect(Collectors.toList());
                } else {
                    idsToExport = selected.stream()
                            .map(RestfulMockResponseDTO::getExtId)
                            .collect(Collectors.toList());
                }

                if (idsToExport.isEmpty()) {
                    return new ByteArrayInputStream(new byte[0]);
                }

                String base64 = importExportService.export(idsToExport, ServerTypeEnum.RESTFUL, userSession.getToken());
                byte[] data = Base64.getDecoder().decode(base64);
                
                return new ByteArrayInputStream(data);
                
            } catch (Exception e) {
                logger.error("Error exporting mocks", e);
                return new ByteArrayInputStream(new byte[0]);
            }
        }));

        startStopButton = new Button("Start");
        startStopButton.addClickListener(e -> toggleServer());
        
        serverStatus = new Span();
        serverStatus.getStyle().set("margin-left", "auto");
        serverStatus.getStyle().set("margin-right", "10px");
        serverStatus.getStyle().set("align-self", "center");

        HorizontalLayout toolbar = new HorizontalLayout(addMockButton, importButton, exportAnchor, serverStatus, startStopButton);
        toolbar.setWidthFull();
        toolbar.addClassName("toolbar");
        toolbar.setAlignItems(Alignment.BASELINE);
        return toolbar;
    }

    private void updateServerStatus() {
        try {
            MockServerState state = mockedServerEngineService.getRestServerState();
            boolean running = state.isRunning();
            
            startStopButton.setText(running ? "Stop Server" : "Start Server");
            startStopButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
            startStopButton.addThemeVariants(running ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_PRIMARY);
            
            serverStatus.setText(running ? "Running on port " + state.getPort() : "Stopped");
            serverStatus.getElement().getThemeList().clear();
            serverStatus.getElement().getThemeList().add(running ? "badge success" : "badge error");
            
        } catch (Exception e) {
            logger.error("Error checking server status", e);
            Notification.show("Error checking server status").addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void toggleServer() {
        try {
            MockServerState state = mockedServerEngineService.getRestServerState();
            if (state.isRunning()) {
                mockedServerEngineService.shutdownRest(userSession.getToken());
            } else {
                mockedServerEngineService.startRest(userSession.getToken());
            }
            updateServerStatus();
        } catch (Exception e) {
            logger.error("Error toggling server", e);
            Notification.show("Error toggling server: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void openImportDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Import Mocks");

        ComboBox<String> importTypeSelector = new ComboBox<>("Import Type");
        importTypeSelector.setItems("sMockin (ZIP)", "OpenAPI (YAML/JSON)", "RAML (YAML)");
        importTypeSelector.setValue("sMockin (ZIP)");
        importTypeSelector.setAllowCustomValue(false);
        importTypeSelector.setWidthFull();

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".zip");
        upload.setWidthFull();

        importTypeSelector.addValueChangeListener(event -> {
            if ("sMockin (ZIP)".equals(event.getValue())) {
                upload.setAcceptedFileTypes(".zip");
            } else if ("OpenAPI (YAML/JSON)".equals(event.getValue())) {
                upload.setAcceptedFileTypes(".yaml", ".yml", ".json");
            } else if ("RAML (YAML)".equals(event.getValue())) {
                upload.setAcceptedFileTypes(".raml", ".yaml", ".yml");
            }
        });

        Checkbox keepExisting = new Checkbox("Keep Existing Mocks", true);

        upload.addSucceededListener(event -> {
            try {
                String selectedType = importTypeSelector.getValue();
                String fileName = event.getFileName();
                String mimeType = event.getMIMEType();
                long contentLength = event.getContentLength();

                org.springframework.web.multipart.MultipartFile multipartFile = new org.springframework.web.multipart.MultipartFile() {
                    @Override public String getName() { return "file"; }
                    @Override public String getOriginalFilename() { return fileName; }
                    @Override public String getContentType() { return mimeType; }
                    @Override public boolean isEmpty() { return contentLength == 0; }
                    @Override public long getSize() { return contentLength; }
                    @Override public byte[] getBytes() throws java.io.IOException { return buffer.getInputStream().readAllBytes(); }
                    @Override public InputStream getInputStream() throws java.io.IOException { return buffer.getInputStream(); }
                    @Override public void transferTo(java.io.File dest) throws java.io.IOException, IllegalStateException {
                        throw new UnsupportedOperationException("Not implemented");
                    }
                };

                String result = "";
                if ("sMockin (ZIP)".equals(selectedType)) {
                    com.smockin.admin.dto.MockImportConfigDTO config = new com.smockin.admin.dto.MockImportConfigDTO();
                    config.setKeepExisting(keepExisting.getValue());
                    result = importExportService.importFile(multipartFile, config, userSession.getToken());
                } else if ("OpenAPI (YAML/JSON)".equals(selectedType)) {
                    com.smockin.admin.dto.MockImportConfigDTO config = new com.smockin.admin.dto.MockImportConfigDTO();
                    config.setKeepExisting(keepExisting.getValue());
                    ApiImportDTO apiImportDTO = new ApiImportDTO(multipartFile, config);
                    apiImportRouter.route(ApiImportTypeEnum.OPENAPI.name(), apiImportDTO, userSession.getToken());
                    result = "OpenAPI import processed";
                } else if ("RAML (YAML)".equals(selectedType)) {
                    com.smockin.admin.dto.MockImportConfigDTO config = new com.smockin.admin.dto.MockImportConfigDTO();
                    config.setKeepExisting(keepExisting.getValue());
                    ApiImportDTO apiImportDTO = new ApiImportDTO(multipartFile, config);
                    apiImportRouter.route(ApiImportTypeEnum.RAML.name(), apiImportDTO, userSession.getToken());
                    result = "RAML import processed";
                }

                Notification.show("Import complete: " + result).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                updateList();
                dialog.close();
            } catch (Exception e) {
                logger.error("Error importing file", e);
                Notification.show("Error importing file: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        VerticalLayout layout = new VerticalLayout(importTypeSelector, upload, keepExisting);
        dialog.add(layout);
        dialog.open();
    }

    private void addMock() {
        HttpMockDialog dialog = new HttpMockDialog(restfulMockService, userSession, null, mock -> updateList());
        dialog.open();
    }

    private void editMock(RestfulMockResponseDTO mock) {
        HttpMockDialog dialog = new HttpMockDialog(restfulMockService, userSession, mock, m -> updateList());
        dialog.open();
    }

    private void deleteMock(RestfulMockResponseDTO mock) {
        if (mock == null) return;
        try {
            restfulMockService.deleteEndpoint(mock.getExtId(), userSession.getToken());
            updateList();
            Notification.show("Mock deleted").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            logger.error("Error deleting mock", e);
            Notification.show("Error deleting mock: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void updateList() {
        try {
            grid.setItems(restfulMockService.loadAll(userSession.getToken()));
        } catch (Exception e) {
            logger.error("Error loading mocks", e);
            Notification.show("Error loading mocks: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
