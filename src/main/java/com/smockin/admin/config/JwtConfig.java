package com.smockin.admin.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "smockin.jwt")
@Getter
@Setter
public class JwtConfig {

    private String secret = "somesobsecuresecretkey";
    private String issuer = "smockin";
    private String subject = "smockin-access";
    private String roleKey = "role";
    private String fullNameKey = "name";
    private String userNameKey = "username";

}
