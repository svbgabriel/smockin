package com.smockin.admin.ui.views.dashboard;

import com.smockin.admin.service.MailMockService;
import com.smockin.admin.service.MockedServerEngineService;
import com.smockin.admin.service.RestfulMockService;
import com.smockin.admin.service.S3MockService;
import com.smockin.admin.ui.layout.MainLayout;
import com.smockin.admin.ui.security.UserSession;
import com.smockin.admin.ui.views.http.HttpMocksView;
import com.smockin.admin.ui.views.mail.MailMocksView;
import com.smockin.admin.ui.views.s3.S3MocksView;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Route(value = "dashboard", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PageTitle("Dashboard | sMockin")
public class DashboardView extends VerticalLayout {

    private final Logger logger = LoggerFactory.getLogger(DashboardView.class);
    private final UserSession userSession;
    private final RestfulMockService restfulMockService;
    private final S3MockService s3MockService;
    private final MailMockService mailMockService;
    private final MockedServerEngineService mockedServerEngineService;

    private ScheduledExecutorService executor;
    private Consumer<Void> httpUpdate;
    private Consumer<Void> s3Update;
    private Consumer<Void> mailUpdate;

    @FunctionalInterface
    interface ServerToggleAction {
        void perform(boolean isRunning) throws Exception;
    }

    public DashboardView(UserSession userSession,
                         RestfulMockService restfulMockService,
                         S3MockService s3MockService,
                         MailMockService mailMockService,
                         MockedServerEngineService mockedServerEngineService) {
        this.userSession = userSession;
        this.restfulMockService = restfulMockService;
        this.s3MockService = s3MockService;
        this.mailMockService = mailMockService;
        this.mockedServerEngineService = mockedServerEngineService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("Welcome, " + (userSession.getFullName() != null ? userSession.getFullName() : userSession.getUsername()));
        add(header);

        FlexLayout cardsLayout = new FlexLayout();
        cardsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        cardsLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        cardsLayout.getStyle().set("gap", "20px");
        cardsLayout.setWidthFull();

        cardsLayout.add(createHttpCard());
        cardsLayout.add(createS3Card());
        cardsLayout.add(createMailCard());

        add(cardsLayout);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            ui.access(() -> {
                if (httpUpdate != null) httpUpdate.accept(null);
                if (s3Update != null) s3Update.accept(null);
                if (mailUpdate != null) mailUpdate.accept(null);
            });
        }, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (executor != null) {
            executor.shutdownNow();
        }
        super.onDetach(detachEvent);
    }

    private Component createHttpCard() {
        return createCard("HTTP Mocks", () -> {
            try {
                return restfulMockService.loadAll(userSession.getToken()).size();
            } catch (Exception e) {
                return 0;
            }
        }, () -> {
            try {
                return mockedServerEngineService.getRestServerState().isRunning();
            } catch (Exception e) {
                return false;
            }
        }, (isRunning) -> {
            if (isRunning) {
                mockedServerEngineService.shutdownRest(userSession.getToken());
            } else {
                mockedServerEngineService.startRest(userSession.getToken());
            }
        }, HttpMocksView.class, updater -> this.httpUpdate = updater, "mocks");
    }

    private Component createS3Card() {
        return createCard("S3 Buckets", () -> {
            try {
                return s3MockService.loadAll(userSession.getToken()).size();
            } catch (Exception e) {
                return 0;
            }
        }, () -> {
            try {
                return mockedServerEngineService.getS3ServerState().isRunning();
            } catch (Exception e) {
                return false;
            }
        }, (isRunning) -> {
            if (isRunning) {
                mockedServerEngineService.shutdownS3(userSession.getToken());
            } else {
                mockedServerEngineService.startS3(userSession.getToken());
            }
        }, S3MocksView.class, updater -> this.s3Update = updater, "buckets");
    }

    private Component createMailCard() {
        return createCard("Mail Inboxes", () -> {
            try {
                return mailMockService.loadAll(userSession.getToken()).size();
            } catch (Exception e) {
                return 0;
            }
        }, () -> {
            try {
                return mockedServerEngineService.getMailServerState().isRunning();
            } catch (Exception e) {
                return false;
            }
        }, (isRunning) -> {
            if (isRunning) {
                mockedServerEngineService.shutdownMail(userSession.getToken());
            } else {
                mockedServerEngineService.startMail(userSession.getToken());
            }
        }, MailMocksView.class, updater -> this.mailUpdate = updater, "inboxes");
    }

    private Component createCard(String title,
                                 java.util.function.Supplier<Integer> countSupplier,
                                 java.util.function.Supplier<Boolean> runningSupplier,
                                 ServerToggleAction toggleAction,
                                 Class<? extends Component> navigationTarget,
                                 Consumer<Consumer<Void>> updaterRegistrar,
                                 String unitName) {
        
        int initialCount = countSupplier.get();
        boolean initialRunning = runningSupplier.get();
        
        AtomicBoolean isRunning = new AtomicBoolean(initialRunning);
        VerticalLayout card = new VerticalLayout();
        card.addClassName("dashboard-card");
        card.setWidth("300px");
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        card.getStyle().set("box-shadow", "var(--lumo-box-shadow-xs)");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.getStyle().set("cursor", "pointer");
        content.addClickListener(e -> UI.getCurrent().navigate(navigationTarget));

        H3 cardTitle = new H3(title);
        cardTitle.getStyle().set("margin-top", "0");

        Paragraph cardSubtitle = new Paragraph(initialCount + " " + unitName);
        cardSubtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span status = new Span(initialRunning ? "Running" : "Stopped");
        status.getElement().getThemeList().add(initialRunning ? "badge success" : "badge error");

        content.add(cardTitle, cardSubtitle, status);

        Button toggleBtn = new Button(initialRunning ? "Stop" : "Start");
        toggleBtn.addThemeVariants(initialRunning ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_PRIMARY);
        toggleBtn.setWidthFull();
        
        Consumer<Void> updater = (v) -> {
            int currentCount = countSupplier.get();
            boolean currentRunning = runningSupplier.get();
            
            cardSubtitle.setText(currentCount + " " + unitName);
            
            isRunning.set(currentRunning);
            status.setText(currentRunning ? "Running" : "Stopped");
            status.getElement().getThemeList().clear();
            status.getElement().getThemeList().add(currentRunning ? "badge success" : "badge error");
            
            toggleBtn.setText(currentRunning ? "Stop" : "Start");
            toggleBtn.removeThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
            toggleBtn.addThemeVariants(currentRunning ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_PRIMARY);
        };
        
        updaterRegistrar.accept(updater);

        toggleBtn.addClickListener(e -> {
            try {
                boolean currentRunning = isRunning.get();
                toggleAction.perform(currentRunning);
                
                // Refresh immediately after action
                updater.accept(null);
                
                Notification.show("Server " + (isRunning.get() ? "started" : "stopped"));
            } catch (Exception ex) {
                logger.error("Error toggling server", ex);
                Notification.show("Error: " + ex.getMessage());
            }
        });

        card.add(content, toggleBtn);
        return card;
    }
}
