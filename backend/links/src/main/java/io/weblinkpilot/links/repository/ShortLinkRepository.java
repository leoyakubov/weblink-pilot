package io.weblinkpilot.links.repository;

import io.weblinkpilot.links.domain.ShortLink;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShortLinkRepository
    extends JpaRepository<ShortLink, Long>, JpaSpecificationExecutor<ShortLink> {
  Optional<ShortLink> findByCode(String code);

  Optional<ShortLink> findByCustomAlias(String customAlias);

  boolean existsByCode(String code);

  boolean existsByCustomAlias(String customAlias);

  Page<ShortLink> findAllByDeletedAtIsNull(Pageable pageable);

  Page<ShortLink> findAllByOwnerUsernameIsNullAndDeletedAtIsNull(Pageable pageable);

  Page<ShortLink> findAllByOwnerUsernameAndDeletedAtIsNull(String ownerUsername, Pageable pageable);

  Page<ShortLink> findAllByOwnerUsernameInAndDeletedAtIsNull(
      List<String> ownerUsernames, Pageable pageable);

  long countByDeletedAtIsNull();

  long countByOwnerUsernameIsNullAndDeletedAtIsNull();

  long countByOwnerUsernameIsNotNullAndDeletedAtIsNull();

  @Query("select coalesce(sum(s.clickCount), 0) from ShortLink s")
  long sumClickCount();

  @Modifying
  @Query("update ShortLink s set s.clickCount = s.clickCount + 1 where s.code = :code")
  int incrementClickCountByCode(@Param("code") String code);

  @Modifying
  @Query("update ShortLink s set s.clickCount = :clickCount where s.code = :code")
  int updateClickCountByCode(@Param("code") String code, @Param("clickCount") long clickCount);

  @Query(
      "select s from ShortLink s where s.expiresAt is not null and s.expiresAt <= :cutoff and s.deletedAt is null")
  List<ShortLink> findExpiredLinksBefore(@Param("cutoff") java.time.OffsetDateTime cutoff);
}
