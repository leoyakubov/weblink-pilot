package io.weblinkpilot.auth.service;

import io.weblinkpilot.auth.config.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final UserAccountService userAccountService;
    private final AuthProperties authProperties;

    public BootstrapAdminRunner(UserAccountService userAccountService, AuthProperties authProperties) {
        this.userAccountService = userAccountService;
        this.authProperties = authProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (authProperties.getBootstrapAdminUsername() == null || authProperties.getBootstrapAdminUsername().isBlank()) {
            return;
        }
        UserAccountService service = userAccountService;
        var admin = service.ensureBootstrapAdmin();
        if (admin != null) {
            log.info("auth.bootstrap.admin username={} role={}", admin.getUsername(), admin.getRole());
        }
    }
}
