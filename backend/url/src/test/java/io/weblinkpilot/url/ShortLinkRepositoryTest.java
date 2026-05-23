package io.weblinkpilot.url;

import static org.assertj.core.api.Assertions.assertThat;

import io.weblinkpilot.url.domain.ShortLink;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = ShortLinkRepositoryTest.TestConfig.class)
@Transactional
class ShortLinkRepositoryTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestConfig {
    }

    @Autowired
    private ShortLinkRepository repository;

    @Test
    void findsByCodeAndAliasAndExistsChecks() {
        ShortLink link = repository.save(new ShortLink(
                "abc123",
                "https://example.com",
                "demo-alias",
                OffsetDateTime.now(ZoneOffset.UTC),
                null
        ));

        assertThat(repository.findByCode("abc123")).isPresent();
        assertThat(repository.findByCustomAlias("demo-alias")).isPresent();
        assertThat(repository.existsByCode(link.getCode())).isTrue();
        assertThat(repository.existsByCustomAlias("demo-alias")).isTrue();
    }
}
