package com.smockin.admin.ui.views.user;

import com.smockin.admin.dto.response.SmockinUserResponseDTO;
import com.smockin.admin.service.SmockinUserService;
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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("Users | sMockin")
@RolesAllowed({"SYS_ADMIN", "ADMIN"})
public class UsersView extends VerticalLayout implements BeforeEnterObserver {

    private final SmockinUserService userService;
    private final UserSession userSession;
    private final Logger logger = LoggerFactory.getLogger(UsersView.class);

    private Grid<SmockinUserResponseDTO> grid = new Grid<>(SmockinUserResponseDTO.class);

    public UsersView(SmockinUserService userService, UserSession userSession) {
        this.userService = userService;
        this.userSession = userSession;

        setSizeFull();
        configureGrid();

        add(getToolbar(), grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!userSession.isAdmin()) {
            event.rerouteTo("dashboard");
            return;
        }
        updateList();
    }

    private void configureGrid() {
        grid.addClassNames("users-grid");
        grid.setSizeFull();
        
        grid.removeAllColumns();
        grid.addColumn(SmockinUserResponseDTO::getUsername).setHeader("Username").setAutoWidth(true);
        grid.addColumn(SmockinUserResponseDTO::getFullName).setHeader("Full Name").setAutoWidth(true);
        grid.addColumn(user -> UIFormattingUtils.formatEnum(user.getRole())).setHeader("Role").setAutoWidth(true);
        grid.addColumn(SmockinUserResponseDTO::getDateCreated).setHeader("Date Created").setAutoWidth(true);

        grid.addComponentColumn(user -> {
            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addClickListener(e -> editUser(user));

            Button pwdButton = new Button(VaadinIcon.KEY.create());
            pwdButton.addClickListener(e -> resetPassword(user));
            pwdButton.setTooltipText("Generate Password Reset Token");

            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e -> deleteUser(user));

            HorizontalLayout actions = new HorizontalLayout(editButton, pwdButton, deleteButton);
            actions.setSpacing(false);
            return actions;
        }).setHeader("Actions");
    }

    private HorizontalLayout getToolbar() {
        Button addUserButton = new Button("New User");
        addUserButton.addClickListener(click -> addUser());

        HorizontalLayout toolbar = new HorizontalLayout(addUserButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void addUser() {
        UserDialog dialog = new UserDialog(userService, userSession, null, user -> updateList());
        dialog.open();
    }

    private void editUser(SmockinUserResponseDTO user) {
        UserDialog dialog = new UserDialog(userService, userSession, user, u -> updateList());
        dialog.open();
    }

    private void deleteUser(SmockinUserResponseDTO user) {
        if (user == null) return;
        try {
            userService.deleteUser(user.getExtId(), userSession.getToken());
            updateList();
            Notification.show("User deleted").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            logger.error("Error deleting user", e);
            Notification.show("Error deleting user: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void resetPassword(SmockinUserResponseDTO user) {
        try {
            String token = userService.issuePasswordResetToken(user.getExtId(), userSession.getToken());
            Notification notification = new Notification("Reset Token: " + token);
            notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
            notification.setDuration(0); // Persistent until clicked
            notification.setPosition(Notification.Position.MIDDLE);
            Button closeBtn = new Button(VaadinIcon.CLOSE.create(), e -> notification.close());
            notification.add(closeBtn);
            notification.open();
        } catch (Exception e) {
            logger.error("Error resetting password", e);
            Notification.show("Error: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void updateList() {
        try {
            grid.setItems(userService.loadAllUsers(userSession.getToken()));
        } catch (Exception e) {
            logger.error("Error loading users", e);
            Notification.show("Error loading users: " + e.getMessage());
        }
    }
}
