package io.weblinkpilot.auth.repository;

import io.weblinkpilot.auth.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @EntityGraph(attributePaths = "user")
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  @Query(
      "select r from RefreshToken r join fetch r.user u where lower(u.username) = lower(:username)")
  List<RefreshToken> findAllByUsername(@Param("username") String username);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @EntityGraph(attributePaths = "user")
  @Query("select r from RefreshToken r join fetch r.user where r.tokenHash in :tokenHashes")
  List<RefreshToken> findAllByTokenHashIn(@Param("tokenHashes") Collection<String> tokenHashes);
}
