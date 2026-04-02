package com.smockin.admin.ui.views.s3;

import com.smockin.admin.service.MockedServerEngineService;
import com.smockin.mockserver.dto.MockServerState;
import com.vaadin.flow.component.html.Span;
import com.smockin.admin.dto.S3MockBucketDTO;
import com.smockin.admin.dto.response.S3MockBucketResponseDTO;
import com.smockin.admin.dto.response.S3MockBucketResponseLiteDTO;
import com.smockin.admin.enums.S3MockTypeEnum;
import com.smockin.admin.service.S3MockService;
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

@Route(value = "s3-mocks", layout = MainLayout.class)
@PageTitle("S3 Mocks | sMockin")
@PermitAll
public class S3MocksView extends VerticalLayout {

    private final S3MockService s3MockService;
    private final MockedServerEngineService mockedServerEngineService;
    private final UserSession userSession;
    private final Logger logger = LoggerFactory.getLogger(S3MocksView.class);

    private Grid<S3MockBucketResponseLiteDTO> grid = new Grid<>(S3MockBucketResponseLiteDTO.class);
    private Button startStopButton;
    private Span serverStatus;

    public S3MocksView(S3MockService s3MockService, MockedServerEngineService mockedServerEngineService, UserSession userSession) {
        this.s3MockService = s3MockService;
        this.mockedServerEngineService = mockedServerEngineService;
        this.userSession = userSession;

        setSizeFull();
        configureGrid();

        add(getToolbar(), grid);
        updateList();
        updateServerStatus();
    }

    private void configureGrid() {
        grid.addClassNames("s3-mocks-grid");
        grid.setSizeFull();
        
        grid.addColumn(S3MockBucketResponseLiteDTO::getBucket).setHeader("Bucket").setAutoWidth(true);
        
        grid.addColumn(bucket -> UIFormattingUtils.formatStatus(bucket.getStatus())).setHeader("Status").setAutoWidth(true);
        
        grid.addColumn(bucket -> UIFormattingUtils.formatEnum(bucket.getSyncMode())).setHeader("Sync Mode").setAutoWidth(true);
        
        grid.addColumn(S3MockBucketResponseLiteDTO::getDateCreated).setHeader("Date Created").setAutoWidth(true);

        grid.addComponentColumn(bucket -> {
            Button contentButton = new Button(VaadinIcon.FOLDER_OPEN.create());
            contentButton.addClickListener(e -> viewContent(bucket));

            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addClickListener(e -> editBucket(bucket));

            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e -> deleteBucket(bucket));

            HorizontalLayout actions = new HorizontalLayout(contentButton, editButton, deleteButton);
            actions.setSpacing(false);
            return actions;
        }).setHeader("Actions");
    }

    private void viewContent(S3MockBucketResponseLiteDTO bucketLite) {
        try {
            S3MockBucketResponseDTO bucket = s3MockService.loadById(bucketLite.getExtId(), userSession.getToken());
            S3BucketContentDialog dialog = new S3BucketContentDialog(bucket, s3MockService, userSession);
            dialog.open();
        } catch (Exception e) {
            logger.error("Error loading bucket content", e);
            Notification.show("Error loading bucket content: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private HorizontalLayout getToolbar() {
        Button addBucketButton = new Button("New Bucket");
        addBucketButton.addClickListener(click -> addBucket());

        startStopButton = new Button("Start");
        startStopButton.addClickListener(e -> toggleServer());
        
        serverStatus = new Span();
        serverStatus.getStyle().set("margin-left", "auto");
        serverStatus.getStyle().set("margin-right", "10px");
        serverStatus.getStyle().set("align-self", "center");

        HorizontalLayout toolbar = new HorizontalLayout(addBucketButton, serverStatus, startStopButton);
        toolbar.setWidthFull();
        toolbar.addClassName("toolbar");
        toolbar.setAlignItems(Alignment.BASELINE);
        return toolbar;
    }
    
    private void updateServerStatus() {
        try {
            MockServerState state = mockedServerEngineService.getS3ServerState();
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
            MockServerState state = mockedServerEngineService.getS3ServerState();
            if (state.isRunning()) {
                mockedServerEngineService.shutdownS3(userSession.getToken());
            } else {
                mockedServerEngineService.startS3(userSession.getToken());
            }
            updateServerStatus();
        } catch (Exception e) {
            logger.error("Error toggling server", e);
            Notification.show("Error toggling server: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void addBucket() {
        S3MockDialog dialog = new S3MockDialog(s3MockService, userSession, null, bucket -> updateList());
        dialog.open();
    }

    private void editBucket(S3MockBucketResponseLiteDTO bucket) {
        S3MockDialog dialog = new S3MockDialog(s3MockService, userSession, bucket, b -> updateList());
        dialog.open();
    }

    private void deleteBucket(S3MockBucketResponseLiteDTO bucket) {
        if (bucket == null) return;
        try {
            s3MockService.deleteS3BucketOrFile(bucket.getExtId(), S3MockTypeEnum.BUCKET, userSession.getToken());
            updateList();
            Notification.show("Bucket deleted").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            logger.error("Error deleting bucket", e);
            Notification.show("Error deleting bucket: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void updateList() {
        try {
            grid.setItems(s3MockService.loadAll(userSession.getToken()));
        } catch (Exception e) {
            logger.error("Error loading buckets", e);
            Notification.show("Error loading buckets: " + e.getMessage());
        }
    }
}
