package io.weblinkpilot.url.web;

import io.weblinkpilot.shared.contracts.CreateLinkRequest;
import io.weblinkpilot.shared.contracts.LinkResponse;
import io.weblinkpilot.url.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlController {

    private final UrlService urlService;
    private final String baseUrl;

    public UrlController(UrlService urlService, @Value("${app.public-base-url:http://localhost:8080}") String baseUrl) {
        this.urlService = urlService;
        this.baseUrl = baseUrl;
    }

    @PostMapping
    public ResponseEntity<LinkResponse> create(@Valid @RequestBody CreateLinkRequest request) {
        return ResponseEntity.ok(urlService.create(request, baseUrl));
    }

    @GetMapping("/{code}")
    public ResponseEntity<LinkResponse> details(@PathVariable String code) {
        return ResponseEntity.ok(urlService.getByCode(code, baseUrl));
    }
}
