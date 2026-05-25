package io.weblinkpilot.url.web;

import io.swagger.v3.oas.annotations.Hidden;
import io.weblinkpilot.url.service.RedirectService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Hidden
@RequestMapping("/r")
public class RedirectController {

  private final RedirectService redirectService;
  private final RequestContextExtractor requestContextExtractor;

  public RedirectController(
      RedirectService redirectService, RequestContextExtractor requestContextExtractor) {
    this.redirectService = redirectService;
    this.requestContextExtractor = requestContextExtractor;
  }

  @GetMapping("/{code}")
  public ResponseEntity<Void> redirect(
      @PathVariable("code") String code, HttpServletRequest request) {
    String originalUrl =
        redirectService.resolveTarget(code, requestContextExtractor.extract(request));
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.LOCATION, URI.create(originalUrl).toString())
        .build();
  }
}
