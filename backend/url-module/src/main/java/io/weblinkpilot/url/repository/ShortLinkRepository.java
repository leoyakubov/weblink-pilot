package io.weblinkpilot.url.repository;

import io.weblinkpilot.url.domain.ShortLink;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {
    Optional<ShortLink> findByCode(String code);

    Optional<ShortLink> findByCustomAlias(String customAlias);

    boolean existsByCode(String code);

    boolean existsByCustomAlias(String customAlias);
}
