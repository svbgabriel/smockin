package com.smockin.admin.ui.views.login;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.smockin.admin.config.JwtConfig;
import com.smockin.admin.dto.AuthDTO;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.admin.service.AuthService;
import com.smockin.admin.ui.security.UserSession;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

@Route("login")
@PageTitle("Login | sMockin")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final AuthService authService;
    private final UserSession userSession;
    private final JwtConfig jwtConfig;
    private final boolean multiUserMode;
    private final Logger logger = LoggerFactory.getLogger(LoginView.class);

    public LoginView(AuthService authService, UserSession userSession, JwtConfig jwtConfig,
                     @Value("${multi.user.mode:false}") boolean multiUserMode) {
        this.authService = authService;
        this.userSession = userSession;
        this.jwtConfig = jwtConfig;
        this.multiUserMode = multiUserMode;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 title = new H1("sMockin");

        LoginForm loginForm = new LoginForm();
        loginForm.addLoginListener(e -> {
            try {
                AuthDTO authDTO = new AuthDTO();
                authDTO.setUsername(e.getUsername());
                authDTO.setPassword(e.getPassword());

                String token = authService.authenticate(authDTO);

                if (token != null) {
                    DecodedJWT decodedJWT = JWT.decode(token);
                    userSession.setToken(token);
                    userSession.setUsername(decodedJWT.getClaim(jwtConfig.getUserNameKey()).asString());
                    userSession.setFullName(decodedJWT.getClaim(jwtConfig.getFullNameKey()).asString());
                    String roleStr = decodedJWT.getClaim(jwtConfig.getRoleKey()).asString();
                    userSession.setRole(SmockinUserRoleEnum.valueOf(roleStr));

                    UI.getCurrent().navigate("dashboard");
                } else {
                    loginForm.setError(true);
                }

            } catch (Exception ex) {
                logger.error("Login failed", ex);
                loginForm.setError(true);
            }
        });

        add(title, loginForm);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!multiUserMode) {
            event.rerouteTo("dashboard");
            return;
        }
        if (userSession.isLoggedIn()) {
            event.rerouteTo("dashboard");
        }
    }
}
