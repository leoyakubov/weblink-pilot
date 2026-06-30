package io.weblinkpilot.shared.ports;

public interface LinkStatisticsService {

  long countActiveLinks();

  long countAnonymousLinks();

  long countOwnedLinks();

  long sumClickCount();
}
