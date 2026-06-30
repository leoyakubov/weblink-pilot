package io.weblinkpilot.shared.ports;

public interface LinkOwnershipLookupService {

  String ownerUsernameForCode(String code);
}
