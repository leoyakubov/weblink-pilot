package io.weblinkpilot.auth.repository;

import io.weblinkpilot.auth.domain.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

  Optional<UserAccount> findByUsername(String username);

  Optional<UserAccount> findByEmailIgnoreCase(String email);

  boolean existsByUsername(String username);

  boolean existsByEmailIgnoreCase(String email);

  @Query("select count(u) from UserAccount u join u.role r where r.name = :roleName")
  long countByRoleName(@Param("roleName") String roleName);

  @Query(
      "select u.username from UserAccount u join u.role r where upper(r.name) = upper(:roleName)")
  List<String> findUsernamesByRoleName(@Param("roleName") String roleName);
}
