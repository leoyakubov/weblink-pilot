package io.weblinkpilot.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private String issuer = AuthDefaults.ISSUER;
    private String jwtSecret;
    private long tokenTtlMinutes = AuthDefaults.TOKEN_TTL_MINUTES;
    private String bootstrapAdminUsername = BootstrapDefaults.ADMIN_USERNAME;
    private String bootstrapAdminPassword = BootstrapDefaults.ADMIN_PASSWORD;
    private String bootstrapAdminRole = BootstrapDefaults.ADMIN_ROLE;
    private String bootstrapUserUsername = BootstrapDefaults.USER_USERNAME;
    private String bootstrapUserPassword = BootstrapDefaults.USER_PASSWORD;
    private String bootstrapUserRole = BootstrapDefaults.USER_ROLE;

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

    public String getBootstrapUserUsername() {
        return bootstrapUserUsername;
    }

    public void setBootstrapUserUsername(String bootstrapUserUsername) {
        this.bootstrapUserUsername = bootstrapUserUsername;
    }

    public String getBootstrapUserPassword() {
        return bootstrapUserPassword;
    }

    public void setBootstrapUserPassword(String bootstrapUserPassword) {
        this.bootstrapUserPassword = bootstrapUserPassword;
    }

    public String getBootstrapUserRole() {
        return bootstrapUserRole;
    }

    public void setBootstrapUserRole(String bootstrapUserRole) {
        this.bootstrapUserRole = bootstrapUserRole;
    }
}
