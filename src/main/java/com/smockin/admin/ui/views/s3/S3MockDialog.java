package com.smockin.admin.ui.views.s3;

import com.smockin.admin.dto.S3MockBucketDTO;
import com.smockin.admin.dto.response.S3MockBucketResponseLiteDTO;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.S3SyncModeEnum;
import com.smockin.admin.service.S3MockService;
import com.smockin.admin.ui.security.UserSession;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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

public class S3MockDialog extends Dialog {

    private final S3MockService s3MockService;
    private final UserSession userSession;
    private final Consumer<S3MockBucketDTO> onSave;
    private final Logger logger = LoggerFactory.getLogger(S3MockDialog.class);

    private TextField bucket = new TextField("Bucket Name");
    private ComboBox<RecordStatusEnum> status = new ComboBox<>("Status");
    private ComboBox<S3SyncModeEnum> syncMode = new ComboBox<>("Sync Mode");

    private Binder<S3MockBucketDTO> binder = new Binder<>(S3MockBucketDTO.class);
    private S3MockBucketDTO currentBucket;
    private boolean isEditMode;
    private String bucketExtId;

    public S3MockDialog(S3MockService s3MockService, UserSession userSession, S3MockBucketResponseLiteDTO bucketToEdit, Consumer<S3MockBucketDTO> onSave) {
        this.s3MockService = s3MockService;
        this.userSession = userSession;
        this.onSave = onSave;
        this.isEditMode = bucketToEdit != null;

        if (isEditMode) {
            this.currentBucket = new S3MockBucketDTO(bucketToEdit.getBucket(), bucketToEdit.getStatus(), bucketToEdit.getSyncMode());
            this.bucketExtId = bucketToEdit.getExtId();
            setHeaderTitle("Edit S3 Bucket");
        } else {
            this.currentBucket = new S3MockBucketDTO();
            this.currentBucket.setStatus(RecordStatusEnum.ACTIVE);
            this.currentBucket.setSyncMode(S3SyncModeEnum.NO_SYNC); // Default?
            setHeaderTitle("New S3 Bucket");
        }

        configureForm();
        createButtons();
        
        binder.readBean(currentBucket);
    }

    private void configureForm() {
        bucket.setPlaceholder("my-bucket");
        bucket.setRequired(true);

        status.setItems(RecordStatusEnum.values());
        status.setItemLabelGenerator(s -> {
            switch (s) {
                case ACTIVE: return "Active";
                case INACTIVE: return "Inactive";
                default: return s.name();
            }
        });
        status.setRequired(true);

        syncMode.setItems(S3SyncModeEnum.values());
        syncMode.setItemLabelGenerator(mode -> {
            switch (mode) {
                case NO_SYNC: return "No Sync";
                case ONE_WAY: return "One Way (Source -> Target)";
                case BI_DIRECTIONAL: return "Bi-Directional";
                default: return mode.name();
            }
        });
        syncMode.setRequired(true);

        binder.forField(bucket)
                .asRequired("Bucket name is required")
                .bind(S3MockBucketDTO::getBucket, S3MockBucketDTO::setBucket);

        binder.forField(status)
                .asRequired("Status is required")
                .bind(S3MockBucketDTO::getStatus, S3MockBucketDTO::setStatus);

        binder.forField(syncMode)
                .asRequired("Sync Mode is required")
                .bind(S3MockBucketDTO::getSyncMode, S3MockBucketDTO::setSyncMode);

        FormLayout formLayout = new FormLayout(bucket, status, syncMode);
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
        if (binder.writeBeanIfValid(currentBucket)) {
            try {
                if (isEditMode) {
                    s3MockService.updateS3Bucket(bucketExtId, currentBucket, userSession.getToken());
                    Notification.show("Bucket updated successfully").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    s3MockService.createS3Bucket(currentBucket, userSession.getToken());
                    Notification.show("Bucket created successfully").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }
                if (onSave != null) {
                    onSave.accept(currentBucket);
                }
                close();
            } catch (Exception e) {
                logger.error("Error saving bucket", e);
                Notification.show("Error saving bucket: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }
    }
}
