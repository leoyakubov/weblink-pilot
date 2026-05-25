package io.weblinkpilot.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@Profile("!test")
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
            log.info("auth.bootstrap.account.seeded username={} role={}", admin.getUsername(), admin.getRoleName());
        }

        var user = userAccountService.ensureBootstrapUser();
        if (user != null) {
            log.info("auth.bootstrap.account.seeded username={} role={}", user.getUsername(), user.getRoleName());
        }
    }
}
