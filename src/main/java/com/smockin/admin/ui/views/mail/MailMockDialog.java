package com.smockin.admin.ui.views.mail;

import com.smockin.admin.dto.MailMockDTO;
import com.smockin.admin.dto.response.MailMockResponseLiteDTO;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.service.MailMockService;
import com.smockin.admin.ui.security.UserSession;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class MailMockDialog extends Dialog {

    private final MailMockService mailMockService;
    private final UserSession userSession;
    private final Consumer<MailMockDTO> onSave;
    private final Logger logger = LoggerFactory.getLogger(MailMockDialog.class);

    private TextField address = new TextField("Address");
    private ComboBox<RecordStatusEnum> status = new ComboBox<>("Status");
    private Checkbox saveReceivedMail = new Checkbox("Save Received Mail");

    private Binder<MailMockDTO> binder = new Binder<>(MailMockDTO.class);
    private MailMockDTO currentMock;
    private boolean isEditMode;
    private String mockExtId;

    public MailMockDialog(MailMockService mailMockService, UserSession userSession, MailMockResponseLiteDTO mockToEdit, Consumer<MailMockDTO> onSave) {
        this.mailMockService = mailMockService;
        this.userSession = userSession;
        this.onSave = onSave;
        this.isEditMode = mockToEdit != null;

        if (isEditMode) {
            this.currentMock = new MailMockDTO(mockToEdit.getAddress(), mockToEdit.getStatus(), mockToEdit.isSaveReceivedMail());
            this.mockExtId = mockToEdit.getExternalId();
            setHeaderTitle("Edit Mail Mock");
        } else {
            this.currentMock = new MailMockDTO();
            this.currentMock.setStatus(RecordStatusEnum.ACTIVE);
            this.currentMock.setSaveReceivedMail(true);
            setHeaderTitle("New Mail Mock");
        }

        configureForm();
        createButtons();
        
        binder.readBean(currentMock);
    }

    private void configureForm() {
        address.setPlaceholder("user@smockin.com");
        address.setRequired(true);

        status.setItems(RecordStatusEnum.values());
        status.setItemLabelGenerator(s -> {
            switch (s) {
                case ACTIVE: return "Active";
                case INACTIVE: return "Inactive";
                default: return s.name();
            }
        });
        status.setRequired(true);

        binder.forField(address)
                .asRequired("Address is required")
                .bind(MailMockDTO::getAddress, MailMockDTO::setAddress);

        binder.forField(status)
                .asRequired("Status is required")
                .bind(MailMockDTO::getStatus, MailMockDTO::setStatus);

        binder.forField(saveReceivedMail)
                .bind(MailMockDTO::isSaveReceivedMail, MailMockDTO::setSaveReceivedMail);

        FormLayout formLayout = new FormLayout(address, status, saveReceivedMail);
        add(formLayout);
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
                    mailMockService.update(mockExtId, currentMock, true, userSession.getToken()); // retainCachedMail = true
                    Notification.show("Mail mock updated successfully").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    mailMockService.create(currentMock, userSession.getToken());
                    Notification.show("Mail mock created successfully").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }
                if (onSave != null) {
                    onSave.accept(currentMock);
                }
                close();
            } catch (Exception e) {
                logger.error("Error saving mail mock", e);
                Notification.show("Error saving mail mock: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }
    }
}
