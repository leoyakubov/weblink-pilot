package io.weblinkpilot.links.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.weblinkpilot.links.cache.UrlCacheService;
import io.weblinkpilot.links.config.ShortLinkProperties;
import io.weblinkpilot.links.criteria.LinkSearchCriteria;
import io.weblinkpilot.links.domain.ShortLink;
import io.weblinkpilot.links.mapper.LinkResponseMapper;
import io.weblinkpilot.links.repository.ShortLinkRepository;
import io.weblinkpilot.links.support.PublicUrlBuilder;
import io.weblinkpilot.shared.api.common.PaginatedResponse;
import io.weblinkpilot.shared.api.links.LinkResponse;
import io.weblinkpilot.shared.ports.LinkAiMetadataService;
import io.weblinkpilot.shared.ports.LinkOwnerMetadataService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = UrlLookupServicePaginationIntegrationTest.TestConfig.class)
@Transactional
class UrlLookupServicePaginationIntegrationTest {

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @EntityScan(basePackageClasses = ShortLink.class)
  @EnableJpaRepositories(basePackageClasses = ShortLinkRepository.class)
  static class TestConfig {}

  @Autowired private ShortLinkRepository repository;

  private UrlLookupService service;

  @BeforeEach
  void setUp() {
    ShortLinkProperties properties = new ShortLinkProperties();
    PublicUrlBuilder publicUrlBuilder = new PublicUrlBuilder(properties);
    LinkOwnerMetadataService ownerMetadataService =
        new LinkOwnerMetadataService() {
          @Override
          public List<String> usernamesByRole(String roleName) {
            if ("USER".equalsIgnoreCase(roleName)) {
              return List.of("alice", "bob");
            }
            return List.of();
          }
        };
    service =
        new UrlLookupService(
            repository,
            new UrlCacheService(repository),
            ownerMetadataService,
            new LinkAiMetadataService() {},
            new LinkResponseMapper(publicUrlBuilder, ownerMetadataService),
            properties);
  }

  @Test
  void paginatesGuestLinksWithExpirationFilter() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    save("guest-active-new", null, now.minusMinutes(1), now.plusDays(1));
    save("guest-active-old", null, now.minusMinutes(2), null);
    save("guest-expired", null, now.minusMinutes(3), now.minusMinutes(1));
    save("owned-active", "alice", now.minusMinutes(4), null);

    PaginatedResponse<LinkResponse> response =
        service.listRecentLinksPage(LinkSearchCriteria.guest("ACTIVE", 0, 1));

    assertThat(response.content())
        .extracting(LinkResponse::code)
        .containsExactly("guest-active-new");
    assertThat(response.page()).isZero();
    assertThat(response.size()).isEqualTo(1);
    assertThat(response.totalElements()).isEqualTo(2);
    assertThat(response.totalPages()).isEqualTo(2);
    assertThat(response.first()).isTrue();
    assertThat(response.last()).isFalse();
  }

  @Test
  void paginatesAdminRoleFiltersAndEmptyRoles() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    save("alice-new", "alice", now.minusMinutes(1), null);
    save("bob-old", "bob", now.minusMinutes(2), null);
    save("admin-link", "admin", now.minusMinutes(3), null);
    save("anonymous-link", null, now.minusMinutes(4), null);

    PaginatedResponse<LinkResponse> users =
        service.listRecentLinksPage(
            LinkSearchCriteria.user("admin", true, null, "USER", null, 0, 10));
    PaginatedResponse<LinkResponse> unknownRole =
        service.listRecentLinksPage(
            LinkSearchCriteria.user("admin", true, null, "MANAGER", null, 0, 10));

    assertThat(users.content())
        .extracting(LinkResponse::code)
        .containsExactly("alice-new", "bob-old");
    assertThat(users.totalElements()).isEqualTo(2);
    assertThat(unknownRole.content()).isEmpty();
    assertThat(unknownRole.totalElements()).isZero();
  }

  @Test
  void paginatesAdminCreatorFiltersForAnonymousAndNamedOwners() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    save("alice-link", "alice", now.minusMinutes(1), null);
    save("anonymous-link", null, now.minusMinutes(2), null);

    PaginatedResponse<LinkResponse> named =
        service.listRecentLinksPage(
            LinkSearchCriteria.user("admin", true, "ALICE", null, null, 0, 10));
    PaginatedResponse<LinkResponse> anonymous =
        service.listRecentLinksPage(
            LinkSearchCriteria.user("admin", true, "anonymous", null, null, 0, 10));

    assertThat(named.content()).extracting(LinkResponse::code).containsExactly("alice-link");
    assertThat(anonymous.content())
        .extracting(LinkResponse::code)
        .containsExactly("anonymous-link");
  }

  private void save(
      String code, String ownerUsername, OffsetDateTime createdAt, OffsetDateTime expiresAt) {
    repository.save(
        new ShortLink(
            code,
            "https://github.com/weblinkpilot/weblink-pilot/" + code,
            null,
            ownerUsername,
            createdAt,
            expiresAt));
  }
}
