package io.weblinkpilot.auth.service;

import io.weblinkpilot.auth.config.RoleNames;
import io.weblinkpilot.auth.repository.UserAccountRepository;
import io.weblinkpilot.shared.contracts.AdminOverviewResponse;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminOverviewService {

  private final UserAccountRepository userAccountRepository;
  private final ShortLinkRepository shortLinkRepository;

  public AdminOverviewService(
      UserAccountRepository userAccountRepository, ShortLinkRepository shortLinkRepository) {
    this.userAccountRepository = userAccountRepository;
    this.shortLinkRepository = shortLinkRepository;
  }

  public AdminOverviewResponse overview() {
    long totalUsers = userAccountRepository.count();
    long adminUsers = userAccountRepository.countByRoleName(RoleNames.ADMIN);
    long totalLinks = shortLinkRepository.count();
    long anonymousLinks = shortLinkRepository.countByOwnerUsernameIsNull();
    long ownedLinks = shortLinkRepository.countByOwnerUsernameIsNotNull();
    long totalClicks = shortLinkRepository.sumClickCount();
    return new AdminOverviewResponse(
        totalUsers, adminUsers, totalLinks, anonymousLinks, ownedLinks, totalClicks);
  }
}
