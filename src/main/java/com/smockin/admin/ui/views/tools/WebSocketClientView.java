package com.smockin.admin.ui.views.tools;

import com.smockin.admin.ui.layout.MainLayout;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Route(value = "websocket-client", layout = MainLayout.class)
@PageTitle("WebSocket Client | sMockin")
@PermitAll
public class WebSocketClientView extends VerticalLayout {

    private final Logger logger = LoggerFactory.getLogger(WebSocketClientView.class);

    private TextField url = new TextField("Server URL");
    private Button connectButton = new Button("Connect");
    private TextArea messageInput = new TextArea("Message");
    private Button sendButton = new Button("Send");
    private TextArea logArea = new TextArea("Log");

    private WebSocketSession session;
    private final StandardWebSocketClient client = new StandardWebSocketClient();

    public WebSocketClientView() {
        setSizeFull();
        setPadding(true);

        add(new H3("WebSocket Client"));
        add(createConnectionLayout());
        add(createMessageLayout());
        add(createLogLayout());
    }

    private HorizontalLayout createConnectionLayout() {
        url.setPlaceholder("ws://localhost:8001/websocket");
        url.setWidth("400px");

        connectButton.addClickListener(e -> toggleConnection());

        HorizontalLayout layout = new HorizontalLayout(url, connectButton);
        layout.setAlignItems(Alignment.BASELINE);
        return layout;
    }

    private VerticalLayout createMessageLayout() {
        messageInput.setWidthFull();
        messageInput.setHeight("100px");

        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.addClickListener(e -> sendMessage());
        sendButton.setEnabled(false);

        return new VerticalLayout(messageInput, sendButton);
    }

    private VerticalLayout createLogLayout() {
        logArea.setWidthFull();
        logArea.setHeightFull();
        logArea.setReadOnly(true);
        return new VerticalLayout(logArea);
    }

    private void toggleConnection() {
        if (session != null && session.isOpen()) {
            disconnect();
        } else {
            connect();
        }
    }

    private void connect() {
        if (url.isEmpty()) {
            Notification.show("URL is required");
            return;
        }

        try {
            session = client.doHandshake(new TextWebSocketHandler() {
                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                    getUI().ifPresent(ui -> ui.access(() -> log("RECEIVED: " + message.getPayload())));
                }

                @Override
                public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
                    getUI().ifPresent(ui -> ui.access(() -> {
                        log("DISCONNECTED: " + status);
                        resetUI();
                    }));
                }
            }, url.getValue()).get();

            log("CONNECTED");
            connectButton.setText("Disconnect");
            sendButton.setEnabled(true);
            url.setReadOnly(true);

        } catch (Exception e) {
            logger.error("Connection failed", e);
            Notification.show("Connection failed: " + e.getMessage());
        }
    }

    private void disconnect() {
        try {
            if (session != null) {
                session.close();
            }
        } catch (Exception e) {
            logger.error("Error closing session", e);
        } finally {
            resetUI();
        }
    }

    private void sendMessage() {
        try {
            if (session != null && session.isOpen()) {
                String msg = messageInput.getValue();
                session.sendMessage(new TextMessage(msg));
                log("SENT: " + msg);
            }
        } catch (Exception e) {
            logger.error("Error sending message", e);
            log("ERROR SENDING: " + e.getMessage());
        }
    }

    private void resetUI() {
        session = null;
        connectButton.setText("Connect");
        sendButton.setEnabled(false);
        url.setReadOnly(false);
    }

    private void log(String msg) {
        logArea.setValue(logArea.getValue() + msg + "\n");
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        disconnect();
        super.onDetach(detachEvent);
    }
}
