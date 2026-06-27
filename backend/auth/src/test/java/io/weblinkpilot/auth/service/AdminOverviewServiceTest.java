package io.weblinkpilot.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.weblinkpilot.auth.domain.Role;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.repository.UserAccountRepository;
import io.weblinkpilot.links.service.UrlStatisticsService;
import io.weblinkpilot.shared.contracts.AdminOverviewResponse;
import io.weblinkpilot.shared.contracts.AdminUserResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminOverviewServiceTest {

  @Mock private UserAccountRepository userAccountRepository;

  @Mock private UrlStatisticsService urlStatisticsService;

  private AdminOverviewService service;

  @BeforeEach
  void setUp() {
    service = new AdminOverviewService(userAccountRepository, urlStatisticsService);
  }

  @Test
  void overviewCombinesRepositoryCounts() {
    when(userAccountRepository.count()).thenReturn(5L);
    when(userAccountRepository.countByRoleName("ADMIN")).thenReturn(1L);
    when(urlStatisticsService.countActiveLinks()).thenReturn(12L);
    when(urlStatisticsService.countAnonymousLinks()).thenReturn(7L);
    when(urlStatisticsService.countOwnedLinks()).thenReturn(5L);
    when(urlStatisticsService.sumClickCount()).thenReturn(99L);

    AdminOverviewResponse response = service.overview();

    assertThat(response.totalUsers()).isEqualTo(5L);
    assertThat(response.adminUsers()).isEqualTo(1L);
    assertThat(response.totalLinks()).isEqualTo(12L);
    assertThat(response.anonymousLinks()).isEqualTo(7L);
    assertThat(response.ownedLinks()).isEqualTo(5L);
    assertThat(response.totalClicks()).isEqualTo(99L);
  }

  @Test
  void usersReturnsReadOnlyUserRowsSortedByUsername() {
    OffsetDateTime createdAt = OffsetDateTime.of(2026, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC);
    UserAccount user =
        new UserAccount(
            "user",
            "hash",
            "user@example.com",
            new Role("USER"),
            true,
            createdAt.plusDays(1),
            null);
    UserAccount admin =
        new UserAccount(
            "admin",
            "hash",
            "admin@example.com",
            new Role("ADMIN"),
            true,
            createdAt,
            createdAt.plusHours(1));
    admin.markLoggedIn(createdAt.plusHours(2));
    when(userAccountRepository.findAll()).thenReturn(List.of(user, admin));

    List<AdminUserResponse> response = service.users();

    assertThat(response).extracting(AdminUserResponse::username).containsExactly("admin", "user");
    assertThat(response.get(0).email()).isEqualTo("admin@example.com");
    assertThat(response.get(0).role()).isEqualTo("ADMIN");
    assertThat(response.get(0).emailVerified()).isTrue();
    assertThat(response.get(0).lastLoginAt()).isEqualTo(createdAt.plusHours(2));
    assertThat(response.get(1).emailVerified()).isFalse();
  }
}
