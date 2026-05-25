package io.weblinkpilot.auth.repository;

import io.weblinkpilot.auth.domain.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

  Optional<UserAccount> findByUsername(String username);

  boolean existsByUsername(String username);

  long countByRoleName(String roleName);
}
