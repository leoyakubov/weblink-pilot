package io.weblinkpilot.shared.ports;

import java.util.List;

public interface LinkOwnerMetadataService {

  default String roleForOwner(String username) {
    return null;
  }

  default List<String> usernamesByRole(String roleName) {
    return List.of();
  }
}
