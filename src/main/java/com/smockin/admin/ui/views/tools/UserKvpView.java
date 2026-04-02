package com.smockin.admin.ui.views.tools;

import com.smockin.admin.dto.UserKeyValueDataDTO;
import com.smockin.admin.service.UserKeyValueDataService;
import com.smockin.admin.ui.layout.MainLayout;
import com.smockin.admin.ui.security.UserSession;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Route(value = "user-kvp", layout = MainLayout.class)
@PageTitle("User Data | sMockin")
@PermitAll
public class UserKvpView extends VerticalLayout {

    private final UserKeyValueDataService service;
    private final UserSession userSession;
    private final Logger logger = LoggerFactory.getLogger(UserKvpView.class);

    private Grid<UserKeyValueDataDTO> grid = new Grid<>(UserKeyValueDataDTO.class);

    public UserKvpView(UserKeyValueDataService service, UserSession userSession) {
        this.service = service;
        this.userSession = userSession;

        setSizeFull();
        configureGrid();

        Button addButton = new Button("New Data", VaadinIcon.PLUS.create(), e -> openDialog(null));
        add(addButton, grid);
        updateList();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.setColumns("key", "value");
        grid.addComponentColumn(item -> {
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openDialog(item));
            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> deleteItem(item));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(editBtn, deleteBtn);
        });
    }

    private void openDialog(UserKeyValueDataDTO item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(item == null ? "New Data" : "Edit Data");

        TextField keyField = new TextField("Key");
        TextField valueField = new TextField("Value");
        
        Binder<UserKeyValueDataDTO> binder = new Binder<>(UserKeyValueDataDTO.class);
        binder.forField(keyField).asRequired().bind(UserKeyValueDataDTO::getKey, UserKeyValueDataDTO::setKey);
        binder.forField(valueField).asRequired().bind(UserKeyValueDataDTO::getValue, UserKeyValueDataDTO::setValue);

        UserKeyValueDataDTO current = item != null ? 
            new UserKeyValueDataDTO(item.getExtId(), item.getKey(), item.getValue()) : 
            new UserKeyValueDataDTO();
        
        binder.readBean(current);

        Button saveBtn = new Button("Save", e -> {
            if (binder.writeBeanIfValid(current)) {
                try {
                    service.save(java.util.Collections.singletonList(current), userSession.getToken());
                    dialog.close();
                    updateList();
                    Notification.show("Saved").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } catch (Exception ex) {
                    Notification.show("Error: " + ex.getMessage());
                }
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        dialog.add(new FormLayout(keyField, valueField), new HorizontalLayout(saveBtn, cancelBtn));
        dialog.open();
    }

    private void deleteItem(UserKeyValueDataDTO item) {
        try {
            service.delete(item.getExtId(), userSession.getToken());
            updateList();
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage());
        }
    }

    private void updateList() {
        try {
            grid.setItems(service.loadAll(userSession.getToken()));
        } catch (Exception e) {
            logger.error("Error loading data", e);
        }
    }
}
