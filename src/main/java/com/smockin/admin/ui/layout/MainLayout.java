package com.smockin.admin.ui.layout;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.smockin.admin.config.JwtConfig;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.admin.service.AuthService;
import com.smockin.admin.ui.security.UserSession;
import com.smockin.admin.ui.utils.CookieUtils;
import com.smockin.admin.ui.views.dashboard.DashboardView;
import com.smockin.admin.ui.views.http.HttpMocksView;
import com.smockin.admin.ui.views.login.LoginView;
import com.smockin.admin.ui.views.mail.MailMocksView;
import com.smockin.admin.ui.views.proxy.ProxyMappingsView;
import com.smockin.admin.ui.views.s3.S3MocksView;
import com.smockin.admin.ui.views.tools.*;
import com.smockin.admin.ui.views.user.UsersView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.theme.lumo.Lumo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;

public class MainLayout extends AppLayout implements BeforeEnterObserver, AfterNavigationObserver {

    private final Logger logger = LoggerFactory.getLogger(MainLayout.class);

    private final UserSession userSession;
    private final AuthService authService;
    private final JwtConfig jwtConfig;
    private final boolean multiUserMode;

    private VerticalLayout drawerLayout;
    private HorizontalLayout breadcrumbLayout;

    public MainLayout(UserSession userSession,
                      AuthService authService,
                      JwtConfig jwtConfig,
                      @Value("${multi.user.mode:false}") boolean multiUserMode) {
        this.userSession = userSession;
        this.authService = authService;
        this.jwtConfig = jwtConfig;
        this.multiUserMode = multiUserMode;

        createHeader();
        createDrawer();
        refreshDrawer();
    }

    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        applyTheme();
    }

    private void applyTheme() {
        if (!userSession.isThemeLoaded()) {
            Optional<String> themeCookie = CookieUtils.getCookie(CookieUtils.THEME_COOKIE_NAME);
            if (themeCookie.isPresent()) {
                boolean isDark = Boolean.parseBoolean(themeCookie.get());
                userSession.setDarkMode(isDark);
            }
            userSession.setThemeLoaded(true);
        }

        getUI().ifPresent(ui -> {
            var themeList = ui.getElement().getThemeList();
            if (userSession.isDarkMode()) {
                if (!themeList.contains(Lumo.DARK)) {
                    themeList.add(Lumo.DARK);
                }
            } else {
                themeList.remove(Lumo.DARK);
            }
        });
    }

    private void createHeader() {
        H1 logo = new H1("sMockin");
        logo.addClassNames("text-l", "m-m");

        breadcrumbLayout = new HorizontalLayout();
        breadcrumbLayout.setPadding(false);
        breadcrumbLayout.setSpacing(true);
        breadcrumbLayout.addClassNames("px-m", "text-s", "text-secondary");

        Button themeToggle = new Button(VaadinIcon.ADJUST.create(), click -> {
            boolean newMode = !userSession.isDarkMode();
            userSession.setDarkMode(newMode);
            CookieUtils.setCookie(CookieUtils.THEME_COOKIE_NAME, String.valueOf(newMode));
            applyTheme();
        });
        themeToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        themeToggle.setTooltipText("Toggle Dark/Light mode");

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, breadcrumbLayout, themeToggle);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames("py-0", "px-m");

        addToNavbar(header);
    }

    private void createDrawer() {
        drawerLayout = new VerticalLayout();
        drawerLayout.setPadding(true);
        drawerLayout.setSpacing(false);
        drawerLayout.getThemeList().set("spacing-s", true);
        addToDrawer(drawerLayout);
    }

    private void addSectionHeader(String title) {
        H1 header = new H1(title);
        header.addClassNames("text-s", "text-secondary", "mt-m", "mb-s", "px-s");
        header.getStyle().set("text-transform", "uppercase");
        header.getStyle().set("font-weight", "600");
        drawerLayout.add(header);
    }

    private void refreshDrawer() {
        drawerLayout.removeAll();

        RouterLink dashboardLink = createLink(VaadinIcon.DASHBOARD, "Dashboard", DashboardView.class);
        drawerLayout.add(dashboardLink);

        addSectionHeader("Mocks");
        drawerLayout.add(createLink(VaadinIcon.GLOBE, "HTTP", HttpMocksView.class));
        drawerLayout.add(createLink(VaadinIcon.CLOUD, "S3", S3MocksView.class));
        drawerLayout.add(createLink(VaadinIcon.ENVELOPE, "Mail", MailMocksView.class));

        addSectionHeader("Tools");
        drawerLayout.add(createLink(VaadinIcon.EYE, "Live Feed", LiveFeedView.class));
        drawerLayout.add(createLink(VaadinIcon.DATABASE, "User Data", UserKvpView.class));
        drawerLayout.add(createLink(VaadinIcon.BROWSER, "HTTP Client", HttpClientView.class));
        drawerLayout.add(createLink(VaadinIcon.EXCHANGE, "WS Client", WebSocketClientView.class));
        drawerLayout.add(createLink(VaadinIcon.CONNECT_O, "Proxy Mappings", ProxyMappingsView.class));

        if (userSession.isAdmin()) {
            addSectionHeader("Admin");
            drawerLayout.add(createLink(VaadinIcon.USERS, "Users", UsersView.class));
            drawerLayout.add(createLink(VaadinIcon.COG, "Server Config", ServerConfigView.class));
        }

        if (multiUserMode) {
            addSectionHeader("Account");
            Button logoutButton = new Button("Logout", VaadinIcon.SIGN_OUT.create());
            logoutButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            logoutButton.setWidthFull();
            logoutButton.getStyle().set("justify-content", "flex-start");
            logoutButton.addClickListener(e -> {
                userSession.clear();
                getUI().ifPresent(ui -> ui.navigate(LoginView.class));
            });
            drawerLayout.add(logoutButton);
        }
    }

    private RouterLink createLink(VaadinIcon icon, String text, Class<? extends Component> navigationTarget) {
        RouterLink link = new RouterLink(navigationTarget);
        HorizontalLayout layout = new HorizontalLayout(icon.create(), new Span(text));
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(true);
        link.add(layout);
        return link;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        breadcrumbLayout.removeAll();
        breadcrumbLayout.add(new Span("Home"));
        
        String path = event.getLocation().getPath();
        if (!path.isEmpty()) {
            breadcrumbLayout.add(VaadinIcon.CHEVRON_RIGHT.create());
            String title = path.replace("-", " ");
            title = title.substring(0, 1).toUpperCase() + title.substring(1);
            breadcrumbLayout.add(new Span(title));
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (userSession.isLoggedIn()) {
            return;
        }

        if (!multiUserMode) {
            try {
                String token = authService.autoLoginDefaultUser();
                if (token != null) {
                    DecodedJWT decodedJWT = JWT.decode(token);
                    userSession.setToken(token);
                    userSession.setUsername(decodedJWT.getClaim(jwtConfig.getUserNameKey()).asString());
                    userSession.setFullName(decodedJWT.getClaim(jwtConfig.getFullNameKey()).asString());
                    String roleStr = decodedJWT.getClaim(jwtConfig.getRoleKey()).asString();
                    userSession.setRole(SmockinUserRoleEnum.valueOf(roleStr));

                    // Refresh the drawer after auto-login to ensure admin links are visible
                    refreshDrawer();
                    return;
                }
            } catch (Exception e) {
                logger.error("Auto login failed", e);
            }
        }

        event.rerouteTo(LoginView.class);
    }
}
