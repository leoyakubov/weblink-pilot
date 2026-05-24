package io.weblinkpilot.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final UserAccountService userAccountService;

    public BootstrapAdminRunner(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @Override
    public void run(ApplicationArguments args) {
        var admin = userAccountService.ensureBootstrapAdmin();
        if (admin != null) {
            log.info("auth.bootstrap.admin username={} role={}", admin.getUsername(), admin.getRoleName());
        }

        var user = userAccountService.ensureBootstrapUser();
        if (user != null) {
            log.info("auth.bootstrap.user username={} role={}", user.getUsername(), user.getRoleName());
        }
    }
}
