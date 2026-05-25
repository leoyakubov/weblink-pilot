package io.weblinkpilot.auth.service;

import io.weblinkpilot.auth.config.RoleNames;
import io.weblinkpilot.auth.domain.Role;
import io.weblinkpilot.auth.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleCatalogService {

  private final RoleRepository roleRepository;
  private volatile Map<String, Role> rolesByName = Map.of();

  public RoleCatalogService(RoleRepository roleRepository) {
    this.roleRepository = roleRepository;
  }

  @PostConstruct
  @Transactional
  public void loadRoles() {
    ensureRolePresent(RoleNames.ADMIN);
    ensureRolePresent(RoleNames.USER);

    Map<String, Role> loaded =
        roleRepository.findAll().stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    role -> normalize(role.getName()), Function.identity(), (left, right) -> left));
    if (loaded.isEmpty()) {
      throw new IllegalStateException("No roles found in app_roles");
    }
    rolesByName = loaded;
  }

  public Role getRequiredRole(String roleName) {
    String normalized = normalize(roleName);
    Role role = rolesByName.get(normalized);
    if (role == null) {
      throw new IllegalStateException("Role not found: " + roleName);
    }
    return role;
  }

  public String getRequiredRoleName(String roleName) {
    return getRequiredRole(roleName).getName();
  }

  public Collection<Role> getRoles() {
    return rolesByName.values();
  }

  private String normalize(String roleName) {
    if (roleName == null) {
      return "";
    }
    return roleName.trim().toUpperCase(Locale.ROOT);
  }

  private void ensureRolePresent(String roleName) {
    String normalized = normalize(roleName);
    if (roleRepository.findByName(normalized).isPresent()) {
      return;
    }
    roleRepository.save(new Role(normalized));
  }
}
