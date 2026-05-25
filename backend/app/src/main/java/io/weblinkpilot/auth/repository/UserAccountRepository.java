package io.weblinkpilot.auth.repository;

import io.weblinkpilot.auth.domain.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

  Optional<UserAccount> findByUsername(String username);

  boolean existsByUsername(String username);

  @Query("select count(u) from UserAccount u join u.role r where r.name = :roleName")
  long countByRoleName(@Param("roleName") String roleName);
}
