package io.weblinkpilot.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.weblinkpilot.auth.repository.UserAccountRepository;
import io.weblinkpilot.shared.contracts.AdminOverviewResponse;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminOverviewServiceTest {

  @Mock private UserAccountRepository userAccountRepository;

  @Mock private ShortLinkRepository shortLinkRepository;

  private AdminOverviewService service;

  @BeforeEach
  void setUp() {
    service = new AdminOverviewService(userAccountRepository, shortLinkRepository);
  }

  @Test
  void overviewCombinesRepositoryCounts() {
    when(userAccountRepository.count()).thenReturn(5L);
    when(userAccountRepository.countByRoleName("ADMIN")).thenReturn(1L);
    when(shortLinkRepository.countByDeletedAtIsNull()).thenReturn(12L);
    when(shortLinkRepository.countByOwnerUsernameIsNullAndDeletedAtIsNull()).thenReturn(7L);
    when(shortLinkRepository.countByOwnerUsernameIsNotNullAndDeletedAtIsNull()).thenReturn(5L);
    when(shortLinkRepository.sumClickCount()).thenReturn(99L);

    AdminOverviewResponse response = service.overview();

    assertThat(response.totalUsers()).isEqualTo(5L);
    assertThat(response.adminUsers()).isEqualTo(1L);
    assertThat(response.totalLinks()).isEqualTo(12L);
    assertThat(response.anonymousLinks()).isEqualTo(7L);
    assertThat(response.ownedLinks()).isEqualTo(5L);
    assertThat(response.totalClicks()).isEqualTo(99L);
  }
}
