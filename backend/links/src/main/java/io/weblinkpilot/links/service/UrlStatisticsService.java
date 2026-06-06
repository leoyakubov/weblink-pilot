package io.weblinkpilot.links.service;

import io.weblinkpilot.links.repository.ShortLinkRepository;
import org.springframework.stereotype.Service;

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
}
