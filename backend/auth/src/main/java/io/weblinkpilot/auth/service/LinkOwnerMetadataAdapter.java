package io.weblinkpilot.auth.service;

import io.weblinkpilot.auth.repository.UserAccountRepository;
import io.weblinkpilot.links.service.LinkOwnerMetadataService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LinkOwnerMetadataAdapter implements LinkOwnerMetadataService {

  private final UserAccountRepository userAccountRepository;

  public LinkOwnerMetadataAdapter(UserAccountRepository userAccountRepository) {
    this.userAccountRepository = userAccountRepository;
  }

  @Override
  public String roleForOwner(String username) {
    if (username == null || username.isBlank()) {
      return null;
    }

    return userAccountRepository.findByUsername(username.trim().toLowerCase()).stream()
        .map(account -> account.getRoleName())
        .findFirst()
        .orElse(null);
  }

  @Override
  public List<String> usernamesByRole(String roleName) {
    if (roleName == null || roleName.isBlank()) {
      return List.of();
    }

    return userAccountRepository.findUsernamesByRoleName(roleName.trim());
  }
}
