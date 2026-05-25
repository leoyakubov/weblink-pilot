package io.weblinkpilot.bootstrap;

import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.url.domain.ShortLink;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@Profile("!test")
public class BootstrapLinkRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapLinkRunner.class);

    private final ShortLinkRepository shortLinkRepository;
    private final AuthProperties authProperties;

    public BootstrapLinkRunner(ShortLinkRepository shortLinkRepository, AuthProperties authProperties) {
        this.shortLinkRepository = shortLinkRepository;
        this.authProperties = authProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        seedLink("welcome", "https://github.com/weblinkpilot/weblink-pilot", null, null, now);
        seedLink("docs", "https://github.com/weblinkpilot/weblink-pilot/tree/main/docs", null, null, now);
        seedLink("user-home", "https://github.com/weblinkpilot/weblink-pilot/issues", null, normalizeUsername(authProperties.getBootstrapUserUsername()), now);
        seedLink("admin-home", "https://github.com/weblinkpilot/weblink-pilot/actions", null, normalizeUsername(authProperties.getBootstrapAdminUsername()), now);
    }

    private void seedLink(String code,
                          String originalUrl,
                          String customAlias,
                          String ownerUsername,
                          OffsetDateTime createdAt) {
        if (shortLinkRepository.existsByCode(code)) {
            return;
        }
        if (customAlias != null && shortLinkRepository.existsByCustomAlias(customAlias)) {
            return;
        }

        ShortLink link = new ShortLink(code, originalUrl, customAlias, ownerUsername, createdAt, null);
        shortLinkRepository.save(link);
        log.info("bootstrap.link.seeded code={} owner={}", code, ownerUsername == null ? "anonymous" : ownerUsername);
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            return null;
        }
        String value = username.trim().toLowerCase(java.util.Locale.ROOT);
        return value.isBlank() ? null : value;
    }
}
