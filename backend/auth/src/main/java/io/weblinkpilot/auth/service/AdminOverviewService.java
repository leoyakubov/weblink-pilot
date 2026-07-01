package io.weblinkpilot.auth.service;

import io.weblinkpilot.auth.config.RoleNames;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.mapper.AuthResponseMapper;
import io.weblinkpilot.auth.repository.UserAccountRepository;
import io.weblinkpilot.shared.api.admin.AdminOverviewResponse;
import io.weblinkpilot.shared.api.admin.AdminUserResponse;
import io.weblinkpilot.shared.api.common.PaginatedResponse;
import io.weblinkpilot.shared.api.links.LinkCreatorOptionResponse;
import io.weblinkpilot.shared.ports.LinkStatisticsService;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class AdminOverviewService {

  private static final int DEFAULT_PAGE_SIZE = 10;
  private static final int MAX_PAGE_SIZE = 100;

  private final UserAccountRepository userAccountRepository;
  private final LinkStatisticsService linkStatisticsService;
  private final AuthResponseMapper responseMapper;

  public AdminOverviewService(
      UserAccountRepository userAccountRepository,
      LinkStatisticsService linkStatisticsService,
      AuthResponseMapper responseMapper) {
    this.userAccountRepository = userAccountRepository;
    this.linkStatisticsService = linkStatisticsService;
    this.responseMapper = responseMapper;
  }

  public AdminOverviewResponse overview() {
    long totalUsers = userAccountRepository.count();
    long adminUsers = userAccountRepository.countByRoleName(RoleNames.ADMIN);
    long totalLinks = linkStatisticsService.countActiveLinks();
    long anonymousLinks = linkStatisticsService.countAnonymousLinks();
    long ownedLinks = linkStatisticsService.countOwnedLinks();
    long totalClicks = linkStatisticsService.sumClickCount();
    return new AdminOverviewResponse(
        totalUsers, adminUsers, totalLinks, anonymousLinks, ownedLinks, totalClicks);
  }

  public List<LinkCreatorOptionResponse> linkCreators() {
    List<LinkCreatorOptionResponse> users =
        userAccountRepository.findAll().stream()
            .map(responseMapper::toCreatorOption)
            .sorted(Comparator.comparing(LinkCreatorOptionResponse::username))
            .toList();
    return java.util.stream.Stream.concat(
            java.util.stream.Stream.of(new LinkCreatorOptionResponse("anonymous", "ANONYMOUS")),
            users.stream())
        .toList();
  }

  public List<AdminUserResponse> users() {
    return usersPage(0, DEFAULT_PAGE_SIZE).content();
  }

  public PaginatedResponse<AdminUserResponse> usersPage(int page, int size) {
    int pageNumber = Math.max(0, page);
    int pageSize = clampPageSize(size);
    Page<UserAccount> users =
        userAccountRepository.findAllByOrderByUsernameAsc(PageRequest.of(pageNumber, pageSize));
    return PaginatedResponse.of(
        users.getContent().stream().map(responseMapper::toAdminUser).toList(),
        users.getNumber(),
        users.getSize(),
        users.getTotalElements());
  }

  private int clampPageSize(int size) {
    if (size <= 0) {
      return DEFAULT_PAGE_SIZE;
    }
    return Math.min(size, MAX_PAGE_SIZE);
  }
}
