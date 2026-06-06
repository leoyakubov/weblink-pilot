package io.weblinkpilot.bootstrap;

import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.service.UserAccountService;
import io.weblinkpilot.links.service.UrlBootstrapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@Profile("!test")
public class BootstrapLinkRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(BootstrapLinkRunner.class);

  private final UserAccountService userAccountService;
  private final UrlBootstrapService urlBootstrapService;

  public BootstrapLinkRunner(
      UserAccountService userAccountService, UrlBootstrapService urlBootstrapService) {
    this.userAccountService = userAccountService;
    this.urlBootstrapService = urlBootstrapService;
  }

  @Override
  public void run(ApplicationArguments args) {
    UserAccount admin = userAccountService.ensureBootstrapAdmin();
    UserAccount user = userAccountService.ensureBootstrapUser();
    urlBootstrapService.seedDefaultLinks(
        user == null ? null : user.getUsername(), admin == null ? null : admin.getUsername());

    log.info(
        "bootstrap.links.seeded user={} admin={}",
        user == null ? "disabled" : user.getUsername(),
        admin == null ? "disabled" : admin.getUsername());
  }
}
