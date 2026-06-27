package io.weblinkpilot.auth.service;

import io.weblinkpilot.auth.config.RoleNames;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.repository.UserAccountRepository;
import io.weblinkpilot.links.service.UrlStatisticsService;
import io.weblinkpilot.shared.contracts.AdminOverviewResponse;
import io.weblinkpilot.shared.contracts.AdminUserResponse;
import io.weblinkpilot.shared.contracts.LinkCreatorOptionResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdminOverviewService {

  private final UserAccountRepository userAccountRepository;
  private final UrlStatisticsService urlStatisticsService;

  public AdminOverviewService(
      UserAccountRepository userAccountRepository, UrlStatisticsService urlStatisticsService) {
    this.userAccountRepository = userAccountRepository;
    this.urlStatisticsService = urlStatisticsService;
  }

  public AdminOverviewResponse overview() {
    long totalUsers = userAccountRepository.count();
    long adminUsers = userAccountRepository.countByRoleName(RoleNames.ADMIN);
    long totalLinks = urlStatisticsService.countActiveLinks();
    long anonymousLinks = urlStatisticsService.countAnonymousLinks();
    long ownedLinks = urlStatisticsService.countOwnedLinks();
    long totalClicks = urlStatisticsService.sumClickCount();
    return new AdminOverviewResponse(
        totalUsers, adminUsers, totalLinks, anonymousLinks, ownedLinks, totalClicks);
  }

  public List<LinkCreatorOptionResponse> linkCreators() {
    List<LinkCreatorOptionResponse> users =
        userAccountRepository.findAll().stream()
            .map(
                account ->
                    new LinkCreatorOptionResponse(account.getUsername(), account.getRoleName()))
            .sorted(Comparator.comparing(LinkCreatorOptionResponse::username))
            .toList();
    return java.util.stream.Stream.concat(
            java.util.stream.Stream.of(new LinkCreatorOptionResponse("anonymous", "ANONYMOUS")),
            users.stream())
        .toList();
  }

  public List<AdminUserResponse> users() {
    return userAccountRepository.findAll().stream()
        .sorted(Comparator.comparing(UserAccount::getUsername))
        .map(
            account ->
                new AdminUserResponse(
                    account.getUsername(),
                    account.getEmail(),
                    account.getRoleName(),
                    account.isEnabled(),
                    account.isEmailVerified(),
                    account.getCreatedAt(),
                    account.getLastLoginAt()))
        .toList();
  }
}
