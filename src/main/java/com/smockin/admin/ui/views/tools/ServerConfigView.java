package com.smockin.admin.ui.views.tools;

import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.service.MockedServerEngineService;
import com.smockin.admin.ui.layout.MainLayout;
import com.smockin.admin.ui.security.UserSession;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Route(value = "server-config", layout = MainLayout.class)
@PageTitle("Server Config | sMockin")
@RolesAllowed({"SYS_ADMIN", "ADMIN"})
public class ServerConfigView extends VerticalLayout {

    private final MockedServerEngineService engineService;
    private final UserSession userSession;
    private final Logger logger = LoggerFactory.getLogger(ServerConfigView.class);

    private final ComboBox<ServerTypeEnum> serverType = new ComboBox<>("Server Type");
    private final IntegerField port = new IntegerField("Port");
    private final IntegerField minThreads = new IntegerField("Min Threads");
    private final IntegerField maxThreads = new IntegerField("Max Threads");
    private final Checkbox autoStart = new Checkbox("Auto Start");

    private final Binder<MockedServerConfigDTO> binder = new Binder<>(MockedServerConfigDTO.class);
    private MockedServerConfigDTO currentConfig;

    public ServerConfigView(MockedServerEngineService engineService, UserSession userSession) {
        this.engineService = engineService;
        this.userSession = userSession;

        setSizeFull();
        setPadding(true);

        add(new H3("Server Configuration"));
        configureForm();
        
        serverType.addValueChangeListener(e -> loadConfig(e.getValue()));
        serverType.setValue(ServerTypeEnum.RESTFUL);
    }

    private void configureForm() {
        serverType.setItems(ServerTypeEnum.values());
        
        binder.forField(port).asRequired("Port is required").bind(MockedServerConfigDTO::getPort, MockedServerConfigDTO::setPort);
        binder.forField(minThreads).bind(MockedServerConfigDTO::getMinThreads, MockedServerConfigDTO::setMinThreads);
        binder.forField(maxThreads).bind(MockedServerConfigDTO::getMaxThreads, MockedServerConfigDTO::setMaxThreads);
        binder.forField(autoStart).bind(MockedServerConfigDTO::isAutoStart, MockedServerConfigDTO::setAutoStart);

        FormLayout form = new FormLayout(serverType, port, minThreads, maxThreads, autoStart);
        
        Button saveButton = new Button("Save", e -> save());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(form, saveButton);
    }

    private void loadConfig(ServerTypeEnum type) {
        if (type == null) return;
        try {
            currentConfig = engineService.loadServerConfig(type);
            binder.readBean(currentConfig);
        } catch (Exception e) {
            logger.error("Error loading config", e);
            Notification.show("Error loading config: " + e.getMessage());
        }
    }

    private void save() {
        if (binder.writeBeanIfValid(currentConfig)) {
            try {
                engineService.saveServerConfig(serverType.getValue(), currentConfig, userSession.getToken());
                Notification.show("Configuration saved").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception e) {
                logger.error("Error saving config", e);
                Notification.show("Error saving config: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }
    }
}
