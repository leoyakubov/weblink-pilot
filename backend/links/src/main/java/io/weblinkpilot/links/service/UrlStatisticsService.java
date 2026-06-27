package io.weblinkpilot.links.service;

import io.weblinkpilot.links.repository.ShortLinkRepository;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrlStatisticsService {

  private final ShortLinkRepository repository;

  public UrlStatisticsService(ShortLinkRepository repository) {
    this.repository = repository;
  }

  public long countActiveLinks() {
    return repository.countByDeletedAtIsNull();
  }

  public long countAnonymousLinks() {
    return repository.countByOwnerUsernameIsNullAndDeletedAtIsNull();
  }

  public long countOwnedLinks() {
    return repository.countByOwnerUsernameIsNotNullAndDeletedAtIsNull();
  }

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
