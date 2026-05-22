package io.weblinkpilot.url.web;

import io.weblinkpilot.shared.contracts.CreateLinkRequest;
import io.weblinkpilot.shared.contracts.LinkResponse;
import io.weblinkpilot.url.service.UrlService;
import io.weblinkpilot.url.service.QrCodeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    private final QrCodeService qrCodeService;
    private final String baseUrl;

    public UrlController(UrlService urlService, QrCodeService qrCodeService, @Value("${app.public-base-url:http://localhost:8080}") String baseUrl) {
        this.urlService = urlService;
        this.qrCodeService = qrCodeService;
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
                                                      "originalUrl": "https://github.com/docs",
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

    @GetMapping(value = "/{code}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(
            summary = "Get QR code for a short link",
            description = "Returns a PNG QR code that encodes the public redirect URL for the short link.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "QR code image",
                            content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE, schema = @Schema(type = "string", format = "binary"))
                    )
            }
    )
    public ResponseEntity<byte[]> qr(@PathVariable String code) {
        String shortUrl = urlService.getByCode(code, baseUrl).shortUrl();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCodeService.generatePng(shortUrl));
    }
}
