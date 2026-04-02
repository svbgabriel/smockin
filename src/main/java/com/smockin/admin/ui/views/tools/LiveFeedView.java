package com.smockin.admin.ui.views.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smockin.admin.dto.response.*;
import com.smockin.admin.enums.LiveLoggingDirectionEnum;
import com.smockin.admin.enums.LiveLoggingMessageTypeEnum;
import com.smockin.admin.service.LiveFeedCacheService;
import com.smockin.admin.ui.layout.MainLayout;
import com.smockin.admin.ui.security.UserSession;
import com.smockin.admin.ui.utils.UIFormattingUtils;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Route(value = "live-feed", layout = MainLayout.class)
@PageTitle("Live Feed | sMockin")
@PermitAll
public class LiveFeedView extends VerticalLayout {

    private final Logger logger = LoggerFactory.getLogger(LiveFeedView.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserSession userSession;
    private final LiveFeedCacheService liveFeedCacheService;

    @Value("${server.port:8000}")
    private int serverPort;

    private Grid<LogEntry> grid = new Grid<>(LogEntry.class);
    private List<LogEntry> logs = new ArrayList<>();
    private ListDataProvider<LogEntry> dataProvider = new ListDataProvider<>(logs);
    
    private Button connectButton = new Button("Connect");
    private Button clearButton = new Button("Clear");
    private Checkbox autoScroll = new Checkbox("Auto Scroll", true);
    
    private ComboBox<String> methodFilter = new ComboBox<>("Method");
    private TextField statusFilter = new TextField("Status");
    private TextField urlFilter = new TextField("URL");

    private WebSocketSession session;
    private StandardWebSocketClient client = new StandardWebSocketClient();

    public LiveFeedView(UserSession userSession, LiveFeedCacheService liveFeedCacheService) {
        this.userSession = userSession;
        this.liveFeedCacheService = liveFeedCacheService;
        setSizeFull();
        setPadding(true);

        add(new H3("Live Feed"));
        add(createFilters());
        add(createToolbar());
        
        configureGrid();
        add(grid);
    }

    private HorizontalLayout createFilters() {
        methodFilter.setItems("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS");
        methodFilter.setClearButtonVisible(true);
        methodFilter.addValueChangeListener(e -> updateFilters());

        statusFilter.setPlaceholder("Filter by status...");
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(e -> updateFilters());

        urlFilter.setPlaceholder("Filter by URL...");
        urlFilter.setClearButtonVisible(true);
        urlFilter.setWidth("300px");
        urlFilter.addValueChangeListener(e -> updateFilters());

        HorizontalLayout filters = new HorizontalLayout(methodFilter, statusFilter, urlFilter);
        filters.setAlignItems(Alignment.BASELINE);
        return filters;
    }

    private void updateFilters() {
        dataProvider.setFilter(entry -> {
            boolean methodMatch = methodFilter.getValue() == null || methodFilter.getValue().equalsIgnoreCase(entry.getMethod());
            boolean statusMatch = statusFilter.isEmpty() || (entry.getStatus() != null && entry.getStatus().contains(statusFilter.getValue()));
            boolean urlMatch = urlFilter.isEmpty() || (entry.getUrl() != null && entry.getUrl().toLowerCase().contains(urlFilter.getValue().toLowerCase()));
            return methodMatch && statusMatch && urlMatch;
        });
    }

    private HorizontalLayout createToolbar() {
        connectButton.addClickListener(e -> toggleConnection());
        clearButton.addClickListener(e -> {
            logs.clear();
            dataProvider.refreshAll();
        });
        return new HorizontalLayout(connectButton, clearButton, autoScroll);
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(log -> UIFormattingUtils.formatString(log.getType())).setHeader("Type").setAutoWidth(true);
        grid.addColumn(LogEntry::getMethod).setHeader("Method").setAutoWidth(true);
        grid.addColumn(LogEntry::getUrl).setHeader("URL").setAutoWidth(true);
        grid.addColumn(LogEntry::getStatus).setHeader("Status").setAutoWidth(true);
        grid.addColumn(LogEntry::getDate).setHeader("Date").setAutoWidth(true);
        
        grid.setDataProvider(dataProvider);
        
        grid.addItemClickListener(event -> showDetails(event.getItem()));
    }

    private void showDetails(LogEntry entry) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Log Details");
        dialog.setWidth("600px");
        dialog.setHeight("500px");

        VerticalLayout layout = new VerticalLayout();
        layout.add(new Span("Type: " + entry.getType()));
        layout.add(new Span("Method: " + entry.getMethod()));
        layout.add(new Span("URL: " + entry.getUrl()));
        layout.add(new Span("Status: " + entry.getStatus()));
        layout.add(new Span("Date: " + entry.getDate()));

        if (entry.headers != null && !entry.headers.isEmpty()) {
            layout.add(new H3("Headers"));
            Pre headersPre = new Pre(entry.headers.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining("\n")));
            layout.add(headersPre);
        }

        if (entry.body != null && !entry.body.isEmpty()) {
            layout.add(new H3("Body"));
            Pre bodyPre = new Pre(entry.body);
            layout.add(bodyPre);
        }

        dialog.add(layout);
        Button closeButton = new Button("Close", e -> dialog.close());
        dialog.getFooter().add(closeButton);
        dialog.open();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        loadCachedLogs();
    }

    private void loadCachedLogs() {
        List<LiveLoggingDTO> cached = liveFeedCacheService.getCachedLogs();
        logs.clear();
        for (LiveLoggingDTO dto : cached) {
            logs.add(mapToLogEntry(dto));
        }
        dataProvider.refreshAll();
    }

    private LogEntry mapToLogEntry(LiveLoggingDTO dto) {
        LogEntry entry = new LogEntry();
        entry.type = dto.getType().name();
        if (dto.getPayload() instanceof LiveLoggingTrafficDTO) {
            LiveLoggingTrafficDTO traffic = (LiveLoggingTrafficDTO) dto.getPayload();
            entry.date = traffic.getDate().toString();
            if (traffic.getContent() != null) {
                entry.url = traffic.getContent().getUrl();
                entry.headers = traffic.getContent().getHeaders();
                entry.body = traffic.getContent().getBody();
                if (traffic.getContent() instanceof LiveLoggingInboundContentDTO) {
                    entry.method = ((LiveLoggingInboundContentDTO) traffic.getContent()).getMethod();
                } else if (traffic.getContent() instanceof LiveLoggingOutboundContentDTO) {
                    entry.status = String.valueOf(((LiveLoggingOutboundContentDTO) traffic.getContent()).getStatus());
                }
            }
        }
        return entry;
    }

    private void toggleConnection() {
        if (session != null && session.isOpen()) {
            disconnect();
        } else {
            connect();
        }
    }

    private void connect() {
        try {
            String token = userSession.getToken();
            if (token == null) token = "dummy";
            String wsUrl = "ws://localhost:" + serverPort + "/liveLoggingFeed/true/" + token;

            this.session = client.doHandshake(new TextWebSocketHandler() {
                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                    getUI().ifPresent(ui -> ui.access(() -> processMessage(message.getPayload())));
                }
                
                @Override
                public void afterConnectionEstablished(WebSocketSession session) {
                     getUI().ifPresent(ui -> ui.access(() -> {
                        connectButton.setText("Disconnect");
                        Notification.show("Connected");
                    }));
                }
                
                @Override
                public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                     getUI().ifPresent(ui -> ui.access(() -> {
                        connectButton.setText("Connect");
                        LiveFeedView.this.session = null;
                    }));
                }
            }, wsUrl).get();
            
        } catch (Exception e) {
            logger.error("Connection failed", e);
            Notification.show("Failed: " + e.getMessage());
        }
    }


    private void disconnect() {
        try {
            if (session != null) session.close();
        } catch (Exception e) {
            logger.error("Error closing", e);
        }
    }

    private void processMessage(String json) {
        try {
            LiveLoggingDTO dto = objectMapper.readValue(json, LiveLoggingDTO.class);
            LogEntry entry = mapToLogEntry(dto);
            
            logs.add(0, entry);
            dataProvider.refreshAll();
            if (autoScroll.getValue()) {
                grid.scrollToIndex(0);
            }
        } catch (Exception e) {
            logger.error("Error processing", e);
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        disconnect();
        super.onDetach(detachEvent);
    }

    public static class LogEntry {
        String type;
        String method;
        String url;
        String status;
        String date;
        Map<String, String> headers;
        String body;
        
        public String getType() { return type; }
        public String getMethod() { return method; }
        public String getUrl() { return url; }
        public String getStatus() { return status; }
        public String getDate() { return date; }
    }
}
