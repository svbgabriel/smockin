package com.smockin.admin.ui.views.http;

import com.smockin.admin.dto.RestfulMockDefinitionDTO;
import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.dto.RuleDTO;
import com.smockin.admin.dto.response.RestfulMockResponseDTO;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.service.RestfulMockService;
import com.smockin.admin.ui.security.UserSession;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.function.Consumer;

public class HttpMockDialog extends Dialog {

    private final RestfulMockService restfulMockService;
    private final UserSession userSession;
    private final Consumer<RestfulMockDTO> onSave;
    private final Logger logger = LoggerFactory.getLogger(HttpMockDialog.class);

    private TextField path = new TextField("Path");
    private ComboBox<RestMethodEnum> method = new ComboBox<>("Method");
    private ComboBox<RecordStatusEnum> status = new ComboBox<>("Status");
    private ComboBox<RestMockTypeEnum> mockType = new ComboBox<>("Type");

    // Proxy Fields
    private IntegerField proxyTimeout = new IntegerField("Proxy Timeout (ms)");
    private IntegerField webSocketTimeout = new IntegerField("WebSocket Timeout (ms)");
    private IntegerField sseHeartBeat = new IntegerField("SSE Heartbeat (ms)");
    private Checkbox proxyPushId = new Checkbox("Push ID on Connect");

    // Stateful Fields
    private TextField statefulIdFieldName = new TextField("Stateful ID Field Name");
    private TextField statefulIdFieldLocation = new TextField("Stateful ID Location");
    private TextArea statefulResponseBody = new TextArea("Default Response Body");

    // JS Fields
    private TextArea customJsSyntax = new TextArea("JavaScript Code");

    private Binder<RestfulMockDTO> binder = new Binder<>(RestfulMockDTO.class);
    private RestfulMockDTO currentMock;
    private boolean isEditMode;
    private String mockExtId;

    private Grid<RestfulMockDefinitionDTO> definitionsGrid = new Grid<>(RestfulMockDefinitionDTO.class);
    private Grid<RuleDTO> rulesGrid = new Grid<>(RuleDTO.class);
    
    private Tab generalTab = new Tab("General");
    private Tab definitionsTab = new Tab("Definitions");
    private Tab rulesTab = new Tab("Rules");
    private Tab jsTab = new Tab("JavaScript");
    
    private VerticalLayout generalTabContent;
    private VerticalLayout definitionsTabContent;
    private VerticalLayout rulesTabContent;
    private VerticalLayout jsTabContent;
    
    private FormLayout proxyLayout;
    private FormLayout statefulLayout;
    
    private Tabs tabs;

    public HttpMockDialog(RestfulMockService restfulMockService, UserSession userSession, RestfulMockResponseDTO mockToEdit, Consumer<RestfulMockDTO> onSave) {
        this.restfulMockService = restfulMockService;
        this.userSession = userSession;
        this.onSave = onSave;
        this.isEditMode = mockToEdit != null;

        if (isEditMode) {
            this.currentMock = mockToEdit;
            this.mockExtId = mockToEdit.getExtId();
            setHeaderTitle("Edit HTTP Mock");
            if (this.currentMock.getRules() == null) this.currentMock.setRules(new ArrayList<>());
            if (this.currentMock.getDefinitions() == null) this.currentMock.setDefinitions(new ArrayList<>());
        } else {
            this.currentMock = new RestfulMockDTO();
            this.currentMock.setMethod(RestMethodEnum.GET);
            this.currentMock.setStatus(RecordStatusEnum.ACTIVE);
            this.currentMock.setMockType(RestMockTypeEnum.SEQ);
            this.currentMock.setDefinitions(new ArrayList<>());
            this.currentMock.setRules(new ArrayList<>());
            setHeaderTitle("New HTTP Mock");
        }

        configureLayouts();
        createButtons();
        
        binder.readBean(currentMock);
        refreshDefinitionsGrid();
        refreshRulesGrid();
        
        updateVisibility();
        
        mockType.addValueChangeListener(e -> updateVisibility());
    }

    private void configureLayouts() {
        tabs = new Tabs(generalTab, definitionsTab, rulesTab, jsTab);

        generalTabContent = createGeneralTab();
        definitionsTabContent = createDefinitionsTab();
        rulesTabContent = createRulesTab();
        jsTabContent = createJsTab();

        definitionsTabContent.setVisible(false);
        rulesTabContent.setVisible(false);
        jsTabContent.setVisible(false);

        tabs.addSelectedChangeListener(event -> {
            generalTabContent.setVisible(generalTab.equals(event.getSelectedTab()));
            definitionsTabContent.setVisible(definitionsTab.equals(event.getSelectedTab()));
            rulesTabContent.setVisible(rulesTab.equals(event.getSelectedTab()));
            jsTabContent.setVisible(jsTab.equals(event.getSelectedTab()));
        });

        add(tabs, generalTabContent, definitionsTabContent, rulesTabContent, jsTabContent);
        setWidth("900px");
        setHeight("700px");
    }

    private VerticalLayout createGeneralTab() {
        path.setPlaceholder("/example/path");
        path.setRequired(true);
        method.setItems(RestMethodEnum.values());
        method.setRequired(true);
        status.setItems(RecordStatusEnum.values());
        status.setRequired(true);
        mockType.setItems(RestMockTypeEnum.values());
        mockType.setItemLabelGenerator(type -> switch (type) {
            case SEQ -> "Sequential";
            case RULE -> "Rule";
            case PROXY_HTTP -> "Proxy HTTP";
            case PROXY_SSE -> "Proxy SSE";
            case PROXY_WS -> "Proxy WebSocket";
            case CUSTOM_JS -> "Custom JavaScript";
            case RULE_WS -> "Rule WebSocket";
            case STATEFUL -> "Stateful";
        });
        mockType.setRequired(true);

        binder.forField(path).asRequired("Path is required").bind(RestfulMockDTO::getPath, RestfulMockDTO::setPath);
        binder.forField(method).asRequired("Method is required").bind(RestfulMockDTO::getMethod, RestfulMockDTO::setMethod);
        binder.forField(status).asRequired("Status is required").bind(RestfulMockDTO::getStatus, RestfulMockDTO::setStatus);
        binder.forField(mockType).asRequired("Type is required").bind(RestfulMockDTO::getMockType, RestfulMockDTO::setMockType);

        // Proxy Fields Binding
        binder.forField(proxyTimeout).bind(dto -> (int)dto.getProxyTimeoutInMillis(), (dto, val) -> dto.setProxyTimeoutInMillis(val != null ? val : 0));
        binder.forField(webSocketTimeout).bind(dto -> (int)dto.getWebSocketTimeoutInMillis(), (dto, val) -> dto.setWebSocketTimeoutInMillis(val != null ? val : 0));
        binder.forField(sseHeartBeat).bind(dto -> (int)dto.getSseHeartBeatInMillis(), (dto, val) -> dto.setSseHeartBeatInMillis(val != null ? val : 0));
        binder.forField(proxyPushId).bind(RestfulMockDTO::isProxyPushIdOnConnect, RestfulMockDTO::setProxyPushIdOnConnect);
        
        proxyLayout = new FormLayout(proxyTimeout, webSocketTimeout, sseHeartBeat, proxyPushId);
        proxyLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        proxyLayout.setVisible(false);

        // Stateful Fields Binding
        binder.forField(statefulIdFieldName).bind(RestfulMockDTO::getStatefulIdFieldName, RestfulMockDTO::setStatefulIdFieldName);
        binder.forField(statefulIdFieldLocation).bind(RestfulMockDTO::getStatefulIdFieldLocation, RestfulMockDTO::setStatefulIdFieldLocation);
        binder.forField(statefulResponseBody).bind(RestfulMockDTO::getStatefulDefaultResponseBody, RestfulMockDTO::setStatefulDefaultResponseBody);
        
        statefulLayout = new FormLayout(statefulIdFieldName, statefulIdFieldLocation, statefulResponseBody);
        statefulLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        statefulLayout.setVisible(false);

        FormLayout mainForm = new FormLayout(path, method, status, mockType);
        
        VerticalLayout layout = new VerticalLayout(mainForm, proxyLayout, statefulLayout);
        layout.setPadding(true);
        return layout;
    }
    
    private VerticalLayout createJsTab() {
        customJsSyntax.setSizeFull();
        customJsSyntax.setHeight("400px");
        customJsSyntax.getStyle().set("font-family", "monospace");
        customJsSyntax.getStyle().set("white-space", "pre");
        binder.forField(customJsSyntax).bind(RestfulMockDTO::getCustomJsSyntax, RestfulMockDTO::setCustomJsSyntax);
        
        VerticalLayout layout = new VerticalLayout(customJsSyntax);
        layout.setSizeFull();
        layout.setPadding(true);
        return layout;
    }

    private VerticalLayout createDefinitionsTab() {
        definitionsGrid.setColumns("orderNo", "httpStatusCode", "responseContentType", "sleepInMillis");
        definitionsGrid.getColumnByKey("orderNo").setHeader("Order");
        definitionsGrid.addComponentColumn(def -> {
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openDefinitionDialog(def));
            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> {
                currentMock.getDefinitions().remove(def);
                refreshDefinitionsGrid();
            });
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(editBtn, deleteBtn);
        });
        Button addDefButton = new Button("Add Definition", VaadinIcon.PLUS.create(), e -> openDefinitionDialog(null));
        VerticalLayout layout = new VerticalLayout(addDefButton, definitionsGrid);
        layout.setSizeFull();
        layout.setPadding(true);
        return layout;
    }

    private VerticalLayout createRulesTab() {
        rulesGrid.setColumns("orderNo", "httpStatusCode", "responseContentType");
        rulesGrid.addComponentColumn(rule -> {
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openRuleDialog(rule));
            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> {
                currentMock.getRules().remove(rule);
                refreshRulesGrid();
            });
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(editBtn, deleteBtn);
        });
        Button addRuleButton = new Button("Add Rule", VaadinIcon.PLUS.create(), e -> openRuleDialog(null));
        VerticalLayout layout = new VerticalLayout(addRuleButton, rulesGrid);
        layout.setSizeFull();
        layout.setPadding(true);
        return layout;
    }

    private void openRuleDialog(RuleDTO rule) {
        RuleDialog dialog = new RuleDialog(rule, savedRule -> {
            if (rule == null) {
                currentMock.getRules().add(savedRule);
            }
            refreshRulesGrid();
        });
        dialog.open();
    }

    private void updateVisibility() {
        RestMockTypeEnum type = mockType.getValue();
        if (type == null) return;
        
        definitionsTab.setVisible(type == RestMockTypeEnum.SEQ || type == RestMockTypeEnum.STATEFUL);
        rulesTab.setVisible(type == RestMockTypeEnum.RULE || type == RestMockTypeEnum.RULE_WS);
        jsTab.setVisible(type == RestMockTypeEnum.CUSTOM_JS);
        
        proxyLayout.setVisible(type == RestMockTypeEnum.PROXY_HTTP || type == RestMockTypeEnum.PROXY_WS || type == RestMockTypeEnum.PROXY_SSE);
        statefulLayout.setVisible(type == RestMockTypeEnum.STATEFUL);
        
        // Refine proxy fields visibility if needed
        sseHeartBeat.setVisible(type == RestMockTypeEnum.PROXY_SSE);
        webSocketTimeout.setVisible(type == RestMockTypeEnum.PROXY_WS);
        proxyTimeout.setVisible(type == RestMockTypeEnum.PROXY_HTTP);
        proxyPushId.setVisible(type == RestMockTypeEnum.PROXY_WS);
        
        // Hide HTTP Status Code column for RULE_WS if grid is initialized
        if (rulesGrid.getColumnByKey("httpStatusCode") != null) {
            rulesGrid.getColumnByKey("httpStatusCode").setVisible(type != RestMockTypeEnum.RULE_WS);
        }

        // Select General tab if the current one is hidden
        Tab selected = tabs.getSelectedTab();
        if (selected != null && !selected.isVisible()) {
            tabs.setSelectedTab(generalTab);
        }
    }

    private void openDefinitionDialog(RestfulMockDefinitionDTO def) {
        RestfulMockDefinitionDialog dialog = new RestfulMockDefinitionDialog(def, savedDef -> {
            if (def == null) {
                currentMock.getDefinitions().add(savedDef);
            }
            refreshDefinitionsGrid();
        });
        dialog.open();
    }

    private void refreshDefinitionsGrid() {
        definitionsGrid.setItems(currentMock.getDefinitions());
    }

    private void refreshRulesGrid() {
        if (currentMock.getRules() != null) rulesGrid.setItems(currentMock.getRules());
    }

    private void createButtons() {
        Button saveButton = new Button("Save", e -> save());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelButton = new Button("Cancel", e -> close());
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.getStyle().set("padding-top", "20px");
        getFooter().add(buttonLayout);
    }

    private void save() {
        if (binder.writeBeanIfValid(currentMock)) {
            try {
                if (isEditMode) {
                    restfulMockService.updateEndpoint(mockExtId, currentMock, userSession.getToken());
                    Notification.show("Mock updated successfully").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    restfulMockService.createEndpoint(currentMock, userSession.getToken());
                    Notification.show("Mock created successfully").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }
                if (onSave != null) onSave.accept(currentMock);
                close();
            } catch (Exception e) {
                logger.error("Error saving mock", e);
                Notification.show("Error saving mock: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }
    }
}
