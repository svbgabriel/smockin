package com.smockin.admin.ui.views.http;

import com.smockin.admin.dto.RestfulMockDefinitionDTO;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

import java.util.function.Consumer;

public class RestfulMockDefinitionDialog extends Dialog {

    private final Consumer<RestfulMockDefinitionDTO> onSave;
    private final RestfulMockDefinitionDTO definition;

    private IntegerField httpStatusCode = new IntegerField("HTTP Status");
    private ComboBox<String> responseContentType = new ComboBox<>("Content Type");
    private TextArea responseBody = new TextArea("Response Body");
    
    private IntegerField sleepInMillis = new IntegerField("Sleep (ms)");
    private Binder<RestfulMockDefinitionDTO> binder = new Binder<>(RestfulMockDefinitionDTO.class);

    public RestfulMockDefinitionDialog(RestfulMockDefinitionDTO definition, Consumer<RestfulMockDefinitionDTO> onSave) {
        this.definition = definition != null ? definition : new RestfulMockDefinitionDTO();
        this.onSave = onSave;

        setHeaderTitle(definition == null ? "New Definition" : "Edit Definition");

        configureForm();
        createButtons();

        binder.readBean(this.definition);
    }

    private void configureForm() {
        httpStatusCode.setValue(200);
        httpStatusCode.setRequiredIndicatorVisible(true);

        responseContentType.setItems("application/json", "text/plain", "text/html", "application/xml");
        responseContentType.setValue("application/json");
        responseContentType.setAllowCustomValue(true);
        
        responseBody.setHeight("300px");
        responseBody.getStyle().set("font-family", "monospace");
        responseBody.getStyle().set("white-space", "pre");
        responseBody.setWidthFull();
        
        sleepInMillis.setValue(0);

        binder.forField(httpStatusCode)
                .asRequired("Status code is required")
                .bind(RestfulMockDefinitionDTO::getHttpStatusCode, RestfulMockDefinitionDTO::setHttpStatusCode);

        binder.forField(responseContentType)
                .asRequired("Content Type is required")
                .bind(RestfulMockDefinitionDTO::getResponseContentType, RestfulMockDefinitionDTO::setResponseContentType);

        binder.forField(responseBody)
                .bind(RestfulMockDefinitionDTO::getResponseBody, RestfulMockDefinitionDTO::setResponseBody);

        binder.forField(sleepInMillis)
                .bind(dto -> (int) dto.getSleepInMillis(), (dto, val) -> dto.setSleepInMillis(val != null ? val : 0));

        FormLayout formLayout = new FormLayout(httpStatusCode, responseContentType, sleepInMillis, responseBody);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        add(formLayout);
        setWidth("600px");
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
        if (binder.writeBeanIfValid(definition)) {
            onSave.accept(definition);
            close();
        }
    }
}
