package io.weblinkpilot.links.service;

import io.weblinkpilot.links.repository.ShortLinkRepository;
import io.weblinkpilot.shared.ports.LinkStatisticsService;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrlStatisticsService implements LinkStatisticsService {

  private final ShortLinkRepository repository;

  public UrlStatisticsService(ShortLinkRepository repository) {
    this.repository = repository;
  }

  @Override
  public long countActiveLinks() {
    return repository.countByDeletedAtIsNull();
  }

  @Override
  public long countAnonymousLinks() {
    return repository.countByOwnerUsernameIsNullAndDeletedAtIsNull();
  }

  @Override
  public long countOwnedLinks() {
    return repository.countByOwnerUsernameIsNotNullAndDeletedAtIsNull();
  }

  @Override
  public long sumClickCount() {
    return repository.sumClickCount();
  }

  @Transactional
  public void syncClickCounts(Map<String, Long> clickCountsByCode) {
    if (clickCountsByCode == null || clickCountsByCode.isEmpty()) {
      return;
    }

    clickCountsByCode.forEach(
        (code, clickCount) -> {
          if (code == null || code.isBlank() || clickCount == null || clickCount < 0) {
            return;
          }
          repository.updateClickCountByCode(code.trim(), clickCount);
        });
  }
}
