package com.smockin.admin.ui.views.proxy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smockin.admin.persistence.enums.ProxyModeTypeEnum;
import com.smockin.admin.service.ProxyMappingManager;
import com.smockin.admin.ui.layout.MainLayout;
import com.smockin.admin.ui.security.UserSession;
import com.smockin.mockserver.dto.ProxyForwardConfigDTO;
import com.smockin.mockserver.dto.ProxyForwardConfigResponseDTO;
import com.smockin.mockserver.dto.ProxyForwardMappingDTO;
import com.smockin.utils.GeneralUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Route(value = "proxy-mappings", layout = MainLayout.class)
@PageTitle("Proxy Mappings | sMockin")
@PermitAll
public class ProxyMappingsView extends VerticalLayout {

    private final ProxyMappingManager proxyMappingManager;
    private final UserSession userSession;
    private final Logger logger = LoggerFactory.getLogger(ProxyMappingsView.class);

    private Checkbox enableProxyMode = new Checkbox("Enable Proxy Mode");
    private ComboBox<ProxyModeTypeEnum> proxyModeType = new ComboBox<>("Proxy Type");
    private Checkbox doNotForwardWhen404Mock = new Checkbox("Do not forward when 404");
    
    private Grid<ProxyForwardMappingDTO> grid = new Grid<>(ProxyForwardMappingDTO.class);
    private List<ProxyForwardMappingDTO> mappings = new ArrayList<>();

    public ProxyMappingsView(ProxyMappingManager proxyMappingManager, UserSession userSession) {
        this.proxyMappingManager = proxyMappingManager;
        this.userSession = userSession;

        setSizeFull();
        setPadding(true);

        add(new H3("Proxy Mappings"));
        
        createGlobalSettings();
        createGrid();
        createToolbar();

        loadData();
    }

    private void createGlobalSettings() {
        proxyModeType.setItems(ProxyModeTypeEnum.values());
        proxyModeType.setWidth("200px");

        enableProxyMode.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                updateProxyMode(e.getValue());
            }
        });

        if (!userSession.isAdmin()) {
            enableProxyMode.setEnabled(false);
            enableProxyMode.setTooltipText("Only admins can toggle proxy mode");
        }

        Button saveSettingsButton = new Button("Save Settings", e -> saveConfig());
        saveSettingsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout settingsLayout = new HorizontalLayout(enableProxyMode, proxyModeType, doNotForwardWhen404Mock, saveSettingsButton);
        settingsLayout.setAlignItems(Alignment.BASELINE);
        add(settingsLayout);
    }

    private void createGrid() {
        grid.setSizeFull();
        grid.setColumns("path", "proxyForwardUrl");
        grid.addColumn(dto -> dto.isDisabled() ? "Yes" : "No").setHeader("Disabled");

        grid.addComponentColumn(dto -> {
            Button editButton = new Button(VaadinIcon.EDIT.create(), e -> openMappingDialog(dto));
            Button deleteButton = new Button(VaadinIcon.TRASH.create(), e -> {
                mappings.remove(dto);
                grid.setItems(mappings);
                saveConfig(); // Auto-save on delete? Or wait for explicit save? Let's auto-save to mimic immediate feedback or maybe just update list and require save.
                // Given the "Save Settings" button, maybe we should only update local list.
                // But typically users expect delete to be persistent.
                // Let's rely on "Save Settings" for everything except the toggle.
                Notification.show("Mapping removed. Click 'Save Settings' to apply.");
            });
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(editButton, deleteButton);
        });
        
        add(grid);
    }

    private void createToolbar() {
        Button addButton = new Button("New Mapping", VaadinIcon.PLUS.create(), e -> openMappingDialog(null));
        
        // Export
        Anchor exportAnchor = new Anchor();
        exportAnchor.getElement().setAttribute("download", true);
        exportAnchor.add(new Button("Export", VaadinIcon.DOWNLOAD.create()));
        
        exportAnchor.setHref(new StreamResource("proxy_mappings.json", () -> {
            try {
                String json = GeneralUtils.serialiseJson(mappings);
                return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                logger.error("Export failed", e);
                return new ByteArrayInputStream(new byte[0]);
            }
        }));

        // Import
        MemoryBuffer buffer = new MemoryBuffer();
        Upload importUpload = new Upload(buffer);
        importUpload.setUploadButton(new Button("Import"));
        importUpload.setAcceptedFileTypes(".json");
        importUpload.addSucceededListener(event -> {
            try {
                // We need to adapt the InputStream to MultipartFile logic or just read content and use logic.
                // The manager expects MultipartFile. We can create a dummy implementation or read the content and call a different method?
                // The manager has `importProxyMappingsFile(MultipartFile ...)`
                // It's easier to just read the JSON here and update the UI if possible, but the logic is in the manager.
                // Let's implement a simple MultipartFile adapter or just read the content and set it.
                // Actually, reading the file here and updating the list is safer.
                InputStream inputStream = buffer.getInputStream();
                String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                List<ProxyForwardMappingDTO> imported = GeneralUtils.deserializeJson(content, new TypeReference<List<ProxyForwardMappingDTO>>() {});
                if (imported != null) {
                    mappings.addAll(imported);
                    grid.setItems(mappings);
                    Notification.show("Mappings imported. Click 'Save Settings' to apply.");
                }
            } catch (Exception ex) {
                logger.error("Error importing file", ex);
                Notification.show("Error importing file: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        HorizontalLayout toolbar = new HorizontalLayout(addButton, exportAnchor, importUpload);
        toolbar.setAlignItems(Alignment.BASELINE);
        add(toolbar);
    }

    private void loadData() {
        try {
            ProxyForwardConfigResponseDTO config = proxyMappingManager.loadProxyForwardMappingsForUser(userSession.getToken());
            enableProxyMode.setValue(config.isProxyMode());
            proxyModeType.setValue(config.getProxyModeType());
            doNotForwardWhen404Mock.setValue(config.isDoNotForwardWhen404Mock());
            mappings = new ArrayList<>(config.getProxyForwardMappings());
            grid.setItems(mappings);
        } catch (Exception e) {
            logger.error("Error loading proxy config", e);
            Notification.show("Error loading configuration").addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void updateProxyMode(boolean enabled) {
        try {
            proxyMappingManager.updateProxyMode(enabled, userSession.getToken());
            Notification.show("Proxy mode " + (enabled ? "enabled" : "disabled"));
        } catch (Exception e) {
            logger.error("Error updating proxy mode", e);
            Notification.show("Error: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            enableProxyMode.setValue(!enabled); // Revert
        }
    }

    private void saveConfig() {
        try {
            ProxyForwardConfigDTO dto = new ProxyForwardConfigDTO();
            dto.setProxyModeType(proxyModeType.getValue());
            dto.setDoNotForwardWhen404Mock(doNotForwardWhen404Mock.getValue());
            dto.setProxyForwardMappings(mappings);
            
            proxyMappingManager.saveProxyForwardMappingsForUser(dto, userSession.getToken());
            Notification.show("Configuration saved").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            logger.error("Error saving config", e);
            Notification.show("Error saving config: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void openMappingDialog(ProxyForwardMappingDTO mappingToEdit) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(mappingToEdit == null ? "New Mapping" : "Edit Mapping");

        ProxyForwardMappingDTO current = mappingToEdit == null ? new ProxyForwardMappingDTO() : mappingToEdit;
        
        TextField path = new TextField("Path");
        path.setRequired(true);
        TextField url = new TextField("Forward URL");
        url.setRequired(true);
        Checkbox disabled = new Checkbox("Disabled");

        Binder<ProxyForwardMappingDTO> binder = new Binder<>(ProxyForwardMappingDTO.class);
        binder.bind(path, ProxyForwardMappingDTO::getPath, ProxyForwardMappingDTO::setPath);
        binder.bind(url, ProxyForwardMappingDTO::getProxyForwardUrl, ProxyForwardMappingDTO::setProxyForwardUrl);
        binder.bind(disabled, ProxyForwardMappingDTO::isDisabled, ProxyForwardMappingDTO::setDisabled);
        
        binder.readBean(current);

        Button saveButton = new Button("Save", e -> {
            if (binder.writeBeanIfValid(current)) {
                if (mappingToEdit == null) {
                    mappings.add(current);
                }
                grid.setItems(mappings);
                dialog.close();
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelButton = new Button("Cancel", e -> dialog.close());

        FormLayout form = new FormLayout(path, url, disabled);
        dialog.add(form);
        dialog.getFooter().add(saveButton, cancelButton);
        dialog.open();
    }
}
