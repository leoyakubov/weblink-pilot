package io.weblinkpilot.analytics.repository;

import io.weblinkpilot.analytics.domain.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    long countByShortCode(String shortCode);
}
