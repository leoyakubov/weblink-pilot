package io.weblinkpilot.links.service;

import io.weblinkpilot.shared.contracts.CreateLinkRequest;
import io.weblinkpilot.shared.contracts.LinkResponse;
import org.springframework.stereotype.Service;

@Service
public class UrlService {

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
}
