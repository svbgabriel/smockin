package com.smockin.admin.ui.views.tools;

import com.smockin.admin.dto.HttpClientCallDTO;
import com.smockin.admin.dto.response.HttpClientResponseDTO;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.service.HttpClientService;
import com.smockin.admin.ui.layout.MainLayout;
import com.smockin.admin.ui.security.UserSession;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Route(value = "http-client", layout = MainLayout.class)
@PageTitle("HTTP Client | sMockin")
@PermitAll
public class HttpClientView extends VerticalLayout {

    private final HttpClientService httpClientService;
    private final UserSession userSession;
    private final Logger logger = LoggerFactory.getLogger(HttpClientView.class);

    private TextField url = new TextField("URL");
    private ComboBox<RestMethodEnum> method = new ComboBox<>("Method");
    private TextArea requestBody = new TextArea("Request Body");
    private VerticalLayout headersLayout = new VerticalLayout();
    private List<HeaderRow> headerRows = new ArrayList<>();
    
    private Span statusLabel = new Span();
    private Span contentTypeLabel = new Span();
    private TextArea responseBody = new TextArea("Response Body");
    private Grid<HttpClientCallDTO> historyGrid = new Grid<>(HttpClientCallDTO.class);

    public HttpClientView(HttpClientService httpClientService, UserSession userSession) {
        this.httpClientService = httpClientService;
        this.userSession = userSession;

        setSizeFull();
        setPadding(true);

        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSizeFull();

        VerticalLayout clientLayout = new VerticalLayout();
        clientLayout.setWidth("70%");
        clientLayout.add(new H3("HTTP Client"));
        clientLayout.add(createRequestLayout());
        clientLayout.add(new H3("Response"));
        clientLayout.add(createResponseLayout());

        VerticalLayout historyLayout = new VerticalLayout();
        historyLayout.setWidth("30%");
        historyLayout.add(new H3("History"));
        configureHistoryGrid();
        historyLayout.add(historyGrid);

        mainLayout.add(clientLayout, historyLayout);
        add(mainLayout);
    }

    private VerticalLayout createRequestLayout() {
        url.setWidthFull();
        url.setPlaceholder("http://localhost:8080/example");
        
        method.setItems(RestMethodEnum.values());
        method.setValue(RestMethodEnum.GET);
        
        requestBody.setWidthFull();
        requestBody.setHeight("150px");
        requestBody.getStyle().set("font-family", "monospace");

        headersLayout.setPadding(false);
        headersLayout.setSpacing(false);
        addHeaderRow();

        Button addHeaderButton = new Button("Add Header", VaadinIcon.PLUS.create(), e -> addHeaderRow());
        addHeaderButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        Button sendButton = new Button("Send", e -> sendRequest());
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        FormLayout form = new FormLayout(url, method);
        form.setColspan(url, 2);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                                new FormLayout.ResponsiveStep("500px", 2));

        VerticalLayout layout = new VerticalLayout(form, new Span("Headers"), headersLayout, addHeaderButton, requestBody, sendButton);
        layout.setPadding(false);
        return layout;
    }

    private void addHeaderRow() {
        HeaderRow row = new HeaderRow();
        headerRows.add(row);
        headersLayout.add(row);
    }

    private void configureHistoryGrid() {
        historyGrid.setSizeFull();
        historyGrid.setColumns("method", "url");
        historyGrid.getColumnByKey("url").setAutoWidth(true);
        
        updateHistory();

        historyGrid.addItemClickListener(event -> {
            HttpClientCallDTO call = event.getItem();
            url.setValue(call.getUrl());
            method.setValue(call.getMethod());
            requestBody.setValue(call.getBody() != null ? call.getBody() : "");
            
            headersLayout.removeAll();
            headerRows.clear();
            if (call.getHeaders() != null) {
                call.getHeaders().forEach((k, v) -> {
                    HeaderRow row = new HeaderRow();
                    row.name.setValue(k);
                    row.value.setValue(v);
                    headerRows.add(row);
                    headersLayout.add(row);
                });
            }
            if (headerRows.isEmpty()) {
                addHeaderRow();
            }
        });
    }

    private void updateHistory() {
        historyGrid.setItems(userSession.getHttpClientHistory());
    }

    private VerticalLayout createResponseLayout() {
        responseBody.setWidthFull();
        responseBody.setHeight("300px");
        responseBody.setReadOnly(true);
        responseBody.getStyle().set("font-family", "monospace");

        HorizontalLayout metaLayout = new HorizontalLayout(new Span("Status: "), statusLabel, new Span(" | Content-Type: "), contentTypeLabel);
        
        VerticalLayout layout = new VerticalLayout(metaLayout, responseBody);
        layout.setPadding(false);
        return layout;
    }

    private void sendRequest() {
        if (url.isEmpty()) {
            Notification.show("URL is required");
            return;
        }

        try {
            HttpClientCallDTO dto = new HttpClientCallDTO(url.getValue(), method.getValue());
            dto.setBody(requestBody.getValue());
            
            Map<String, String> headers = new HashMap<>();
            for (HeaderRow row : headerRows) {
                if (!row.name.isEmpty()) {
                    headers.put(row.name.getValue(), row.value.getValue());
                }
            }
            dto.setHeaders(headers);

            HttpClientResponseDTO response = httpClientService.handleCallToMock(dto);

            statusLabel.setText(String.valueOf(response.getStatus()));
            if (response.getStatus() >= 200 && response.getStatus() < 300) {
                statusLabel.getStyle().set("color", "green");
            } else {
                statusLabel.getStyle().set("color", "red");
            }
            
            contentTypeLabel.setText(response.getContentType());
            
            String body = response.getBody() != null ? response.getBody() : "";
            if (response.getContentType() != null && response.getContentType().contains("application/json")) {
                try {
                    Object json = new com.fasterxml.jackson.databind.ObjectMapper().readValue(body, Object.class);
                    body = new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(json);
                } catch (Exception e) {
                    // ignore and use original body
                }
            }
            responseBody.setValue(body);

            userSession.addHttpClientHistory(dto);
            updateHistory();

        } catch (Exception e) {
            logger.error("Error sending request", e);
            Notification.show("Error: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private class HeaderRow extends HorizontalLayout {
        TextField name = new TextField();
        TextField value = new TextField();
        Button remove = new Button(VaadinIcon.TRASH.create());

        HeaderRow() {
            name.setPlaceholder("Header Name");
            value.setPlaceholder("Value");
            remove.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            remove.addClickListener(e -> {
                headersLayout.remove(this);
                headerRows.remove(this);
            });
            add(name, value, remove);
            setAlignItems(Alignment.BASELINE);
        }
    }
}
