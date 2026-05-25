package io.weblinkpilot.url.web;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.weblinkpilot.shared.contracts.CreateLinkRequest;
import io.weblinkpilot.shared.contracts.LinkResponse;
import io.weblinkpilot.shared.contracts.RedirectPreviewResponse;
import io.weblinkpilot.url.service.PublicUrlBuilder;
import io.weblinkpilot.url.service.QrCodeService;
import io.weblinkpilot.url.service.UrlLookupService;
import io.weblinkpilot.url.service.UrlService;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlController {

  private static final Logger log = LoggerFactory.getLogger(UrlController.class);

  private final UrlService urlService;
  private final UrlLookupService urlLookupService;
  private final QrCodeService qrCodeService;
  private final PublicUrlBuilder publicUrlBuilder;
  private final Counter browseCounter;
  private final Counter detailsCounter;
  private final Counter previewCounter;
  private final Counter qrCounter;

  public UrlController(
      UrlService urlService,
      UrlLookupService urlLookupService,
      QrCodeService qrCodeService,
      PublicUrlBuilder publicUrlBuilder,
      MeterRegistry meterRegistry) {
    this.urlService = urlService;
    this.urlLookupService = urlLookupService;
    this.qrCodeService = qrCodeService;
    this.publicUrlBuilder = publicUrlBuilder;
    this.browseCounter =
        Counter.builder("weblinkpilot.urls.browse.requests")
            .description("Number of recent link browse requests")
            .register(meterRegistry);
    this.detailsCounter =
        Counter.builder("weblinkpilot.urls.details.requests")
            .description("Number of link detail lookups")
            .register(meterRegistry);
    this.previewCounter =
        Counter.builder("weblinkpilot.urls.preview.requests")
            .description("Number of redirect preview requests")
            .register(meterRegistry);
    this.qrCounter =
        Counter.builder("weblinkpilot.urls.qr.requests")
            .description("Number of QR code requests")
            .register(meterRegistry);
  }

  @PostMapping
  @Operation(
      summary = "Create a short link",
      description = "Creates a short link with an optional custom alias and expiration date.",
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = CreateLinkRequest.class),
                      examples = {
                        @ExampleObject(
                            name = "Random default code",
                            value =
                                """
                                                    {
                                                      "originalUrl": "https://github.com/weblinkpilot/weblink-pilot/about",
                                                      "expiresAt": "2026-08-31T23:59:59Z"
                                                    }
                                                    """),
                        @ExampleObject(
                            name = "Custom alias",
                            value =
                                """
                                                    {
                                                      "originalUrl": "https://github.com/weblinkpilot/weblink-pilot/tree/main/docs",
                                                      "customAlias": "docs-team",
                                                      "expiresAt": "2026-12-31T23:59:59Z"
                                                    }
                                                    """)
                      })))
  public ResponseEntity<LinkResponse> create(
      Authentication authentication, @Valid @RequestBody CreateLinkRequest request) {
    if (isAuthenticated(authentication)) {
      return ResponseEntity.ok(urlService.create(request, currentUsername(authentication)));
    }
    return ResponseEntity.ok(urlService.create(request));
  }

  @GetMapping
  public ResponseEntity<List<LinkResponse>> list(
      Authentication authentication, @RequestParam(name = "limit", defaultValue = "10") int limit) {
    browseCounter.increment();
    if (isAuthenticated(authentication)) {
      return ResponseEntity.ok(
          urlLookupService.listRecentLinks(
              currentUsername(authentication), isAdmin(authentication), limit));
    }
    return ResponseEntity.ok(urlLookupService.listRecentLinks(limit));
  }

  @GetMapping("/{code}")
  public ResponseEntity<LinkResponse> details(@PathVariable("code") String code) {
    detailsCounter.increment();
    return ResponseEntity.ok(urlLookupService.getByCode(code));
  }

  @GetMapping("/{code}/preview")
  @Operation(
      summary = "Preview redirect target",
      description =
          "Returns a JSON preview of the redirect target without issuing an actual HTTP redirect.")
  public ResponseEntity<RedirectPreviewResponse> preview(@PathVariable("code") String code) {
    previewCounter.increment();
    LinkResponse response = urlLookupService.getByCode(code);
    return ResponseEntity.ok(
        new RedirectPreviewResponse(
            response.code(),
            response.shortUrl(),
            response.originalUrl(),
            302,
            response.originalUrl()));
  }

  @GetMapping(value = "/{code}/qr", produces = MediaType.IMAGE_PNG_VALUE)
  @Operation(
      summary = "Get QR code for a short link",
      description =
          "Returns a PNG QR code that encodes the public redirect URL for the short link.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "QR code image",
            content =
                @Content(
                    mediaType = MediaType.IMAGE_PNG_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
      })
  public ResponseEntity<byte[]> qr(@PathVariable("code") String code) {
    qrCounter.increment();
    byte[] png = qrCodeService.generatePng(publicUrlBuilder.buildQrScanUrl(code));
    log.info("url.qr.generated code={} bytes={}", code, png.length);
    return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
  }

  private String currentUsername(Authentication authentication) {
    if (!isAuthenticated(authentication)) {
      return null;
    }
    return authentication.getName();
  }

  private boolean isAdmin(Authentication authentication) {
    return isAuthenticated(authentication)
        && authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
  }

  private boolean isAuthenticated(Authentication authentication) {
    return authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken);
  }
}
