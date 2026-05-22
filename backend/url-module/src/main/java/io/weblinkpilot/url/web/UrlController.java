package io.weblinkpilot.url.web;

import io.weblinkpilot.shared.contracts.CreateLinkRequest;
import io.weblinkpilot.shared.contracts.LinkResponse;
import io.weblinkpilot.url.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Operation(
            summary = "Create a short link",
            description = "Creates a short link with an optional custom alias and expiration date.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateLinkRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Branded link",
                                            value = """
                                                    {
                                                      "originalUrl": "https://github.com/openai",
                                                      "customAlias": "github-org",
                                                      "expiresAt": "2026-12-31T23:59:59Z"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Auto-generated alias",
                                            value = """
                                                    {
                                                      "originalUrl": "https://google.com/about",
                                                      "expiresAt": "2026-08-31T23:59:59Z"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    public ResponseEntity<LinkResponse> create(@Valid @RequestBody CreateLinkRequest request) {
        return ResponseEntity.ok(urlService.create(request, baseUrl));
    }

    @GetMapping("/{code}")
    public ResponseEntity<LinkResponse> details(@PathVariable String code) {
        return ResponseEntity.ok(urlService.getByCode(code, baseUrl));
    }
}
