package io.weblinkpilot.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.weblinkpilot.auth.repository.UserAccountRepository;
import io.weblinkpilot.links.service.UrlStatisticsService;
import io.weblinkpilot.shared.contracts.AdminOverviewResponse;
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
}
