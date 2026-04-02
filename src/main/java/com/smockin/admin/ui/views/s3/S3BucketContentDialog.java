package com.smockin.admin.ui.views.s3;

import com.smockin.admin.dto.S3MockDirDTO;
import com.smockin.admin.dto.response.S3MockBucketResponseDTO;
import com.smockin.admin.dto.response.S3MockDirResponseDTO;
import com.smockin.admin.dto.response.S3MockFileResponseDTO;
import com.smockin.admin.enums.S3MockTypeEnum;
import com.smockin.admin.service.S3MockService;
import com.smockin.admin.ui.security.UserSession;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.smockin.admin.ui.utils.SimpleMultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class S3BucketContentDialog extends Dialog {

    private final S3MockService s3MockService;
    private final UserSession userSession;
    private final Logger logger = LoggerFactory.getLogger(S3BucketContentDialog.class);

    private S3MockBucketResponseDTO bucket;
    private S3MockDirResponseDTO currentDir; // null means root
    private List<S3Item> items = new ArrayList<>();
    private Grid<S3Item> grid = new Grid<>(S3Item.class);
    private Button upButton;
    private TextField currentPathField;

    public S3BucketContentDialog(S3MockBucketResponseDTO bucket, S3MockService s3MockService, UserSession userSession) {
        this.bucket = bucket;
        this.s3MockService = s3MockService;
        this.userSession = userSession;

        setHeaderTitle("Bucket Content: " + bucket.getBucket());
        setSizeFull();
        setWidth("900px");
        setHeight("700px");

        configureGrid();
        createToolbar();
        
        loadContent();

        add(getToolbar(), grid);
    }

    private void configureGrid() {
        grid.removeAllColumns();
        grid.addComponentColumn(item -> {
            if (item.isDirectory) {
                return VaadinIcon.FOLDER.create();
            } else {
                return VaadinIcon.FILE.create();
            }
        }).setHeader("Type").setAutoWidth(true);
        
        grid.addColumn(S3Item::getName).setHeader("Name");
        
        grid.addComponentColumn(item -> {
            HorizontalLayout actions = new HorizontalLayout();
            
            if (item.isDirectory) {
                Button openBtn = new Button(VaadinIcon.FOLDER_OPEN.create(), e -> navigateTo(item.dir));
                actions.add(openBtn);
            }
            
            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> deleteItem(item));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            actions.add(deleteBtn);
            
            return actions;
        }).setHeader("Actions");
    }

    private HorizontalLayout getToolbar() {
        upButton = new Button(VaadinIcon.ARROW_UP.create(), e -> navigateUp());
        upButton.setEnabled(false);

        currentPathField = new TextField();
        currentPathField.setReadOnly(true);
        currentPathField.setValue("/");
        currentPathField.setWidthFull();

        Button newFolderBtn = new Button("New Folder", VaadinIcon.PLUS.create(), e -> createFolder());
        
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setUploadButton(new Button("Upload File", VaadinIcon.UPLOAD.create()));
        upload.addSucceededListener(event -> {
            try {
                uploadFile(event.getFileName(), event.getMIMEType(), buffer.getInputStream());
                upload.clearFileList();
            } catch (Exception ex) {
                Notification.show("Upload failed: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        HorizontalLayout toolbar = new HorizontalLayout(upButton, currentPathField, newFolderBtn, upload);
        toolbar.setWidthFull();
        toolbar.setFlexGrow(1, currentPathField);
        return toolbar;
    }

    private void createToolbar() {
        // Already handled in getToolbar
    }

    private void loadContent() {
        items.clear();
        if (currentDir == null) {
            // Root
            items.addAll(bucket.getChildren().stream().map(d -> new S3Item(d)).collect(Collectors.toList()));
            items.addAll(bucket.getFiles().stream().map(f -> new S3Item(f)).collect(Collectors.toList()));
            currentPathField.setValue("/");
            upButton.setEnabled(false);
        } else {
            items.addAll(currentDir.getChildren().stream().map(d -> new S3Item(d)).collect(Collectors.toList()));
            items.addAll(currentDir.getFiles().stream().map(f -> new S3Item(f)).collect(Collectors.toList()));
            currentPathField.setValue(getPath(currentDir));
            upButton.setEnabled(true);
        }
        grid.setItems(items);
    }

    private String getPath(S3MockDirResponseDTO dir) {
        // TODO: Build full path logic (requires parent traversal or storing path)
        return dir.getName(); // Simplified
    }

    private void navigateTo(S3MockDirResponseDTO dir) {
        currentDir = dir;
        loadContent();
    }

    private void navigateUp() {
        if (currentDir == null) return;
        // Logic to find parent is tricky with current DTO structure unless we search from root
        // Simplifying: Refresh bucket from server to get clean state and navigation
        refreshBucketAndNavigate(currentDir.getParentDirExtId());
    }

    private void refreshBucketAndNavigate(String targetDirExtId) {
        try {
            this.bucket = s3MockService.loadById(bucket.getExtId(), userSession.getToken());
            if (targetDirExtId == null) {
                currentDir = null;
            } else {
                currentDir = findDir(bucket.getChildren(), targetDirExtId);
            }
            loadContent();
        } catch (Exception e) {
            logger.error("Error refreshing bucket", e);
        }
    }

    private S3MockDirResponseDTO findDir(List<S3MockDirResponseDTO> dirs, String extId) {
        for (S3MockDirResponseDTO d : dirs) {
            if (d.getExtId().equals(extId)) return d;
            S3MockDirResponseDTO found = findDir(d.getChildren(), extId);
            if (found != null) return found;
        }
        return null;
    }

    private void createFolder() {
        Dialog dialog = new Dialog();
        TextField nameField = new TextField("Folder Name");
        Button createBtn = new Button("Create", e -> {
            try {
                S3MockDirDTO dto = new S3MockDirDTO();
                dto.setName(nameField.getValue());
                dto.setBucketExtId(bucket.getExtId());
                if (currentDir != null) {
                    dto.setParentDirExtId(currentDir.getExtId());
                }
                s3MockService.createS3BucketDir(dto, userSession.getToken());
                dialog.close();
                refreshBucketAndNavigate(currentDir != null ? currentDir.getExtId() : null);
                Notification.show("Folder created").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.add(new VerticalLayout(nameField, createBtn));
        dialog.open();
    }

    private void uploadFile(String filename, String mimeType, InputStream inputStream) throws IOException {
        try {
            MultipartFile multipartFile = new SimpleMultipartFile("file", filename, mimeType, inputStream);
            String parentId = currentDir != null ? currentDir.getExtId() : bucket.getExtId();
            S3MockTypeEnum type = currentDir != null ? S3MockTypeEnum.DIR : S3MockTypeEnum.BUCKET;
            
            s3MockService.uploadS3BucketFile(parentId, type, multipartFile, userSession.getToken());
            refreshBucketAndNavigate(currentDir != null ? currentDir.getExtId() : null);
            Notification.show("File uploaded").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
             throw new IOException(e);
        }
    }

    private void deleteItem(S3Item item) {
        try {
            S3MockTypeEnum type = item.isDirectory ? S3MockTypeEnum.DIR : S3MockTypeEnum.FILE;
            s3MockService.deleteS3BucketOrFile(item.extId, type, userSession.getToken());
            refreshBucketAndNavigate(currentDir != null ? currentDir.getExtId() : null);
            Notification.show("Item deleted").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    public static class S3Item {
        public String name;
        public String extId;
        public boolean isDirectory;
        public S3MockDirResponseDTO dir;
        public S3MockFileResponseDTO file;

        public S3Item(S3MockDirResponseDTO dir) {
            this.name = dir.getName();
            this.extId = dir.getExtId();
            this.isDirectory = true;
            this.dir = dir;
        }

        public S3Item(S3MockFileResponseDTO file) {
            this.name = file.getName();
            this.extId = file.getExtId();
            this.isDirectory = false;
            this.file = file;
        }
        
        public String getName() { return name; }
    }
}
