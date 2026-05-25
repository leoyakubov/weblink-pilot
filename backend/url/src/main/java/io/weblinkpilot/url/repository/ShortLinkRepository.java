package io.weblinkpilot.url.repository;

import io.weblinkpilot.url.domain.ShortLink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {
    Optional<ShortLink> findByCode(String code);

    Optional<ShortLink> findByCustomAlias(String customAlias);

    boolean existsByCode(String code);

    boolean existsByCustomAlias(String customAlias);

    Page<ShortLink> findAllByOwnerUsernameIsNull(Pageable pageable);

    Page<ShortLink> findAllByOwnerUsername(String ownerUsername, Pageable pageable);

    long countByOwnerUsernameIsNull();

    long countByOwnerUsernameIsNotNull();

    @Query("select coalesce(sum(s.clickCount), 0) from ShortLink s")
    long sumClickCount();

    @Modifying
    @Query("update ShortLink s set s.clickCount = s.clickCount + 1 where s.code = :code")
    int incrementClickCountByCode(@Param("code") String code);
}
