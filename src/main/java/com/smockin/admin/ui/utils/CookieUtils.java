package com.smockin.admin.ui.utils;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import jakarta.servlet.http.Cookie;
import java.util.Arrays;
import java.util.Optional;

public class CookieUtils {

    public static final String THEME_COOKIE_NAME = "smockin-dark-mode";

    public static void setCookie(String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24 * 30); // 30 days
        VaadinResponse.getCurrent().addCookie(cookie);
    }

    public static Optional<String> getCookie(String name) {
        Cookie[] cookies = VaadinRequest.getCurrent().getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
