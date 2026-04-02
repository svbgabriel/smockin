package com.smockin.admin.ui.security;

import com.smockin.admin.dto.HttpClientCallDTO;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Component
@VaadinSessionScope
@Getter
@Setter
public class UserSession implements Serializable {

    private String token;
    private String username;
    private String fullName;
    private SmockinUserRoleEnum role;
    private List<HttpClientCallDTO> httpClientHistory = new ArrayList<>();
    private boolean darkMode = false;
    private boolean themeLoaded = false;

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        this.themeLoaded = true;
    }

    public boolean isThemeLoaded() {
        return themeLoaded;
    }

    public boolean isLoggedIn() {
        return token != null;
    }

    public void clear() {
        this.token = null;
        this.username = null;
        this.fullName = null;
        this.role = null;
        this.httpClientHistory.clear();
    }

    public void addHttpClientHistory(HttpClientCallDTO call) {
        // Keep only the last 20 calls
        if (httpClientHistory.size() >= 20) {
            httpClientHistory.remove(httpClientHistory.size() - 1);
        }
        httpClientHistory.add(0, call);
    }

    public boolean isAdmin() {
        return SmockinUserRoleEnum.SYS_ADMIN.equals(role) || SmockinUserRoleEnum.ADMIN.equals(role);
    }
}
