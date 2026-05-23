package io.weblinkpilot.url.service;

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

    public LinkResponse getByCode(String code) {
        return lookupService.getByCode(code);
    }
}
