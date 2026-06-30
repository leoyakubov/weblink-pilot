package io.weblinkpilot.links.service;

import io.weblinkpilot.shared.api.links.CreateLinkRequest;
import io.weblinkpilot.shared.api.links.LinkResponse;
import io.weblinkpilot.shared.ports.LinkOwnershipLookupService;
import org.springframework.stereotype.Service;

@Service
public class UrlService implements LinkOwnershipLookupService {

  private final UrlCreationService creationService;
  private final UrlLookupService lookupService;

  public UrlService(UrlCreationService creationService, UrlLookupService lookupService) {
    this.creationService = creationService;
    this.lookupService = lookupService;
  }

  public LinkResponse create(CreateLinkRequest request) {
    return creationService.create(request);
  }

  public LinkResponse create(CreateLinkRequest request, String ownerUsername) {
    return creationService.create(request, ownerUsername);
  }

  public LinkResponse getByCode(String code) {
    return lookupService.getByCode(code);
  }

  @Override
  public String ownerUsernameForCode(String code) {
    return getByCode(code).ownerUsername();
  }

  public java.util.List<LinkResponse> listRecentLinks(int limit) {
    return lookupService.listRecentLinks(limit);
  }

  public java.util.List<LinkResponse> listRecentLinks(
      String ownerUsername, boolean admin, int limit) {
    return lookupService.listRecentLinks(ownerUsername, admin, limit);
  }

  public java.util.List<LinkResponse> listRecentLinks(
      String ownerUsername, boolean admin, String creatorFilter, int limit) {
    return lookupService.listRecentLinks(ownerUsername, admin, creatorFilter, limit);
  }

  public java.util.List<LinkResponse> listRecentLinks(
      String ownerUsername, boolean admin, String creatorFilter, String ownerRole, int limit) {
    return lookupService.listRecentLinks(ownerUsername, admin, creatorFilter, ownerRole, limit);
  }

  public java.util.List<LinkResponse> listRecentLinks(
      String ownerUsername,
      boolean admin,
      String creatorFilter,
      String ownerRole,
      String expirationFilter,
      int limit) {
    return lookupService.listRecentLinks(
        ownerUsername, admin, creatorFilter, ownerRole, expirationFilter, limit);
  }
}
