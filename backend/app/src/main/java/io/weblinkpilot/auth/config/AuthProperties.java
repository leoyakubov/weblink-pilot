package io.weblinkpilot.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private String issuer = "weblink-pilot";
    private String jwtSecret = "weblink-pilot-demo-secret-change-me-0123456789";
    private long tokenTtlMinutes = 240;
    private String bootstrapAdminUsername = "admin";
    private String bootstrapAdminPassword = "admin123";
    private String bootstrapAdminRole = "ADMIN";

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getTokenTtlMinutes() {
        return tokenTtlMinutes;
    }

    public void setTokenTtlMinutes(long tokenTtlMinutes) {
        this.tokenTtlMinutes = tokenTtlMinutes;
    }

    public String getBootstrapAdminUsername() {
        return bootstrapAdminUsername;
    }

    public void setBootstrapAdminUsername(String bootstrapAdminUsername) {
        this.bootstrapAdminUsername = bootstrapAdminUsername;
    }

    public String getBootstrapAdminPassword() {
        return bootstrapAdminPassword;
    }

    public void setBootstrapAdminPassword(String bootstrapAdminPassword) {
        this.bootstrapAdminPassword = bootstrapAdminPassword;
    }

    public String getBootstrapAdminRole() {
        return bootstrapAdminRole;
    }

    public void setBootstrapAdminRole(String bootstrapAdminRole) {
        this.bootstrapAdminRole = bootstrapAdminRole;
    }
}
