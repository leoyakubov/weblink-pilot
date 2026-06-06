package io.weblinkpilot.auth.repository;

import io.weblinkpilot.auth.domain.SocialIdentity;
import io.weblinkpilot.auth.domain.SocialLoginProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SocialIdentityRepository extends JpaRepository<SocialIdentity, Long> {

  Optional<SocialIdentity> findByProviderAndProviderUserId(
      SocialLoginProvider provider, String providerUserId);

  @Query(
      "select s from SocialIdentity s join fetch s.user u where lower(u.username) = lower(:username) order by s.createdAt asc")
  List<SocialIdentity> findAllByUsername(@Param("username") String username);
}
