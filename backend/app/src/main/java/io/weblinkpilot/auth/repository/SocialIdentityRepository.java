package io.weblinkpilot.auth.repository;

import io.weblinkpilot.auth.domain.SocialIdentity;
import io.weblinkpilot.auth.domain.SocialLoginProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialIdentityRepository extends JpaRepository<SocialIdentity, Long> {

  Optional<SocialIdentity> findByProviderAndProviderUserId(
      SocialLoginProvider provider, String providerUserId);
}
