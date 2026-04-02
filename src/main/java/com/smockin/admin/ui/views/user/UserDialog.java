package com.smockin.admin.ui.views.user;

import com.smockin.admin.dto.SmockinNewUserDTO;
import com.smockin.admin.dto.SmockinUserDTO;
import com.smockin.admin.dto.response.SmockinUserResponseDTO;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.ui.security.UserSession;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class UserDialog extends Dialog {

    private final SmockinUserService userService;
    private final UserSession userSession;
    private final Consumer<SmockinUserDTO> onSave;
    private final Logger logger = LoggerFactory.getLogger(UserDialog.class);

    private TextField username = new TextField("Username");
    private TextField fullName = new TextField("Full Name");
    private ComboBox<SmockinUserRoleEnum> role = new ComboBox<>("Role");
    private PasswordField password = new PasswordField("Password");

    private Binder<SmockinNewUserDTO> binder = new Binder<>(SmockinNewUserDTO.class);
    private SmockinNewUserDTO currentUser;
    private boolean isEditMode;
    private String userExtId;

    public UserDialog(SmockinUserService userService, UserSession userSession, SmockinUserResponseDTO userToEdit, Consumer<SmockinUserDTO> onSave) {
        this.userService = userService;
        this.userSession = userSession;
        this.onSave = onSave;
        this.isEditMode = userToEdit != null;

        if (isEditMode) {
            // Mapping response to DTO for binder
            this.currentUser = new SmockinNewUserDTO(userToEdit.getUsername(), userToEdit.getFullName(), userToEdit.getRole(), null);
            this.userExtId = userToEdit.getExtId();
            setHeaderTitle("Edit User");
        } else {
            this.currentUser = new SmockinNewUserDTO();
            this.currentUser.setRole(SmockinUserRoleEnum.REGULAR);
            setHeaderTitle("New User");
        }

        configureForm();
        createButtons();
        
        binder.readBean(currentUser);
    }

    private void configureForm() {
        username.setRequired(true);
        fullName.setRequired(true);
        
        role.setItems(SmockinUserRoleEnum.values());
        role.setRequired(true);
        
        if (isEditMode) {
            password.setVisible(false);
        } else {
            password.setRequired(true);
        }

        binder.forField(username).asRequired("Username is required").bind(SmockinUserDTO::getUsername, SmockinUserDTO::setUsername);
        binder.forField(fullName).asRequired("Full Name is required").bind(SmockinUserDTO::getFullName, SmockinUserDTO::setFullName);
        binder.forField(role).asRequired("Role is required").bind(SmockinUserDTO::getRole, SmockinUserDTO::setRole);
        
        if (!isEditMode) {
            binder.forField(password).asRequired("Password is required").bind(SmockinNewUserDTO::getPassword, SmockinNewUserDTO::setPassword);
        }

        FormLayout formLayout = new FormLayout(username, fullName, role, password);
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
        if (binder.writeBeanIfValid(currentUser)) {
            try {
                if (isEditMode) {
                    // Update uses SmockinUserDTO (no password)
                    SmockinUserDTO updateDto = new SmockinUserDTO(currentUser.getUsername(), currentUser.getFullName(), currentUser.getRole());
                    userService.updateUser(userExtId, updateDto, userSession.getToken());
                    Notification.show("User updated successfully").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    userService.createUser(currentUser, userSession.getToken());
                    Notification.show("User created successfully").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }
                if (onSave != null) {
                    onSave.accept(currentUser);
                }
                close();
            } catch (Exception e) {
                logger.error("Error saving user", e);
                Notification.show("Error saving user: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }
    }
}
