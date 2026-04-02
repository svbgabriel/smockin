package com.smockin.admin.interceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor);
    }

    /*
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        final String viewName = "forward:/index.html";

        registry.addViewController("/").setViewName(viewName);
        registry.addViewController("/login").setViewName(viewName);
        registry.addViewController("/reset_password").setViewName(viewName);
        registry.addViewController("/dashboard").setViewName(viewName);
        registry.addViewController("/tcp_endpoint").setViewName(viewName);
        registry.addViewController("/s3_endpoint").setViewName(viewName);
        registry.addViewController("/mail_endpoint").setViewName(viewName);
        registry.addViewController("/manage_users").setViewName(viewName);
        registry.addViewController("/manage_user_kvp_data").setViewName(viewName);
        registry.addViewController("/account").setViewName(viewName);
        registry.addViewController("/http_client").setViewName(viewName);
        registry.addViewController("/ws_client").setViewName(viewName);
        registry.addViewController("/live_feed").setViewName(viewName);
    }
    */
}
