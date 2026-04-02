package com.smockin.admin.ui.views.mail;

import com.smockin.admin.dto.response.MailMockMessageResponseDTO;
import com.smockin.admin.dto.response.MailMockResponseDTO;
import com.smockin.admin.dto.response.MailMockResponseLiteDTO;
import com.smockin.admin.service.MailMockService;
import com.smockin.admin.ui.security.UserSession;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class MailMessagesDialog extends Dialog {

    private final MailMockService mailMockService;
    private final UserSession userSession;
    private final MailMockResponseLiteDTO mockLite;
    private final Logger logger = LoggerFactory.getLogger(MailMessagesDialog.class);

    private Grid<MailMockMessageResponseDTO> grid = new Grid<>(MailMockMessageResponseDTO.class);

    public MailMessagesDialog(MailMockResponseLiteDTO mockLite, MailMockService mailMockService, UserSession userSession) {
        this.mockLite = mockLite;
        this.mailMockService = mailMockService;
        this.userSession = userSession;

        setHeaderTitle("Messages for: " + mockLite.getAddress());
        setSizeFull();
        setWidth("900px");
        setHeight("700px");

        configureGrid();
        loadMessages();

        add(grid);
        
        Button closeButton = new Button("Close", e -> close());
        getFooter().add(closeButton);
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        
        grid.addColumn(MailMockMessageResponseDTO::getFrom).setHeader("From").setAutoWidth(true);
        grid.addColumn(MailMockMessageResponseDTO::getSubject).setHeader("Subject").setAutoWidth(true);
        grid.addColumn(MailMockMessageResponseDTO::getDateReceived).setHeader("Received").setAutoWidth(true);
        
        grid.addComponentColumn(msg -> {
            Button viewBtn = new Button("View Body", e -> viewBody(msg));
            return viewBtn;
        });
    }

    private void loadMessages() {
        try {
            // Loading page 0, search "", etc. Simplified.
            MailMockResponseDTO fullMock = mailMockService.loadByIdWithFilteredMessages(
                    mockLite.getExternalId(), 
                    Optional.empty(), 
                    Optional.empty(), 
                    Optional.empty(), 
                    0, 
                    null, 
                    userSession.getToken());
            
            if (fullMock.getMessages() != null && fullMock.getMessages().getPageData() != null) {
                grid.setItems(fullMock.getMessages().getPageData());
            }
        } catch (Exception e) {
            logger.error("Error loading messages", e);
        }
    }

    private void viewBody(MailMockMessageResponseDTO msg) {
        Dialog bodyDialog = new Dialog();
        bodyDialog.setHeaderTitle("Message Body");
        
        Pre bodyContent = new Pre(msg.getBody());
        bodyContent.getStyle().set("overflow", "auto");
        bodyContent.setSizeFull();
        
        VerticalLayout layout = new VerticalLayout(bodyContent);
        layout.setSizeFull();
        bodyDialog.add(layout);
        
        bodyDialog.setWidth("600px");
        bodyDialog.setHeight("500px");
        
        Button closeBtn = new Button("Close", e -> bodyDialog.close());
        bodyDialog.getFooter().add(closeBtn);
        
        bodyDialog.open();
    }
}
