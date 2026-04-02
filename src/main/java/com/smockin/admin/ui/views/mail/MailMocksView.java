package com.smockin.admin.ui.views.mail;

import com.smockin.admin.service.MockedServerEngineService;
import com.smockin.mockserver.dto.MockServerState;
import com.vaadin.flow.component.html.Span;
import com.smockin.admin.dto.response.MailMockResponseLiteDTO;
import com.smockin.admin.service.MailMockService;
import com.smockin.admin.ui.layout.MainLayout;
import com.smockin.admin.ui.security.UserSession;
import com.smockin.admin.ui.utils.UIFormattingUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Route(value = "mail-mocks", layout = MainLayout.class)
@PageTitle("Mail Mocks | sMockin")
@PermitAll
public class MailMocksView extends VerticalLayout {

    private final MailMockService mailMockService;
    private final MockedServerEngineService mockedServerEngineService;
    private final UserSession userSession;
    private final Logger logger = LoggerFactory.getLogger(MailMocksView.class);

    private Grid<MailMockResponseLiteDTO> grid = new Grid<>(MailMockResponseLiteDTO.class);
    private Button startStopButton;
    private Span serverStatus;

    public MailMocksView(MailMockService mailMockService, MockedServerEngineService mockedServerEngineService, UserSession userSession) {
        this.mailMockService = mailMockService;
        this.mockedServerEngineService = mockedServerEngineService;
        this.userSession = userSession;

        setSizeFull();
        configureGrid();

        add(getToolbar(), grid);
        updateList();
        updateServerStatus();
    }

    private void configureGrid() {
        grid.addClassNames("mail-mocks-grid");
        grid.setSizeFull();
        
        grid.removeAllColumns();
        grid.addColumn(MailMockResponseLiteDTO::getAddress).setHeader("Address").setAutoWidth(true);
        grid.addColumn(mock -> UIFormattingUtils.formatStatus(mock.getStatus())).setHeader("Status").setAutoWidth(true);
        grid.addColumn(MailMockResponseLiteDTO::getDateCreated).setHeader("Date Created").setAutoWidth(true);
        grid.addColumn(MailMockResponseLiteDTO::getMessageCount).setHeader("Messages").setAutoWidth(true);

        grid.addComponentColumn(mock -> {
            Button messagesButton = new Button(VaadinIcon.ENVELOPE.create());
            messagesButton.addClickListener(e -> viewMessages(mock));

            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addClickListener(e -> editMock(mock));

            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e -> deleteMock(mock));

            HorizontalLayout actions = new HorizontalLayout(messagesButton, editButton, deleteButton);
            actions.setSpacing(false);
            return actions;
        }).setHeader("Actions");
    }

    private HorizontalLayout getToolbar() {
        Button addMockButton = new Button("New Mail Mock");
        addMockButton.addClickListener(click -> addMock());

        startStopButton = new Button("Start");
        startStopButton.addClickListener(e -> toggleServer());
        
        serverStatus = new Span();
        serverStatus.getStyle().set("margin-left", "auto");
        serverStatus.getStyle().set("margin-right", "10px");
        serverStatus.getStyle().set("align-self", "center");

        HorizontalLayout toolbar = new HorizontalLayout(addMockButton, serverStatus, startStopButton);
        toolbar.setWidthFull();
        toolbar.addClassName("toolbar");
        toolbar.setAlignItems(Alignment.BASELINE);
        return toolbar;
    }
    
    private void updateServerStatus() {
        try {
            MockServerState state = mockedServerEngineService.getMailServerState();
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
            MockServerState state = mockedServerEngineService.getMailServerState();
            if (state.isRunning()) {
                mockedServerEngineService.shutdownMail(userSession.getToken());
            } else {
                mockedServerEngineService.startMail(userSession.getToken());
            }
            updateServerStatus();
        } catch (Exception e) {
            logger.error("Error toggling server", e);
            Notification.show("Error toggling server: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void addMock() {
        MailMockDialog dialog = new MailMockDialog(mailMockService, userSession, null, mock -> updateList());
        dialog.open();
    }

    private void editMock(MailMockResponseLiteDTO mock) {
        MailMockDialog dialog = new MailMockDialog(mailMockService, userSession, mock, m -> updateList());
        dialog.open();
    }

    private void deleteMock(MailMockResponseLiteDTO mock) {
        if (mock == null) return;
        try {
            mailMockService.delete(mock.getExternalId(), userSession.getToken());
            updateList();
            Notification.show("Mail mock deleted").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            logger.error("Error deleting mail mock", e);
            Notification.show("Error deleting mail mock: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void viewMessages(MailMockResponseLiteDTO mock) {
        MailMessagesDialog dialog = new MailMessagesDialog(mock, mailMockService, userSession);
        dialog.open();
    }

    private void updateList() {
        try {
            grid.setItems(mailMockService.loadAll(userSession.getToken()));
        } catch (Exception e) {
            logger.error("Error loading mail mocks", e);
            Notification.show("Error loading mail mocks: " + e.getMessage());
        }
    }
}
