package io.weblinkpilot.ai.web;

import io.weblinkpilot.ai.service.AiLinkMetadataService;
import io.weblinkpilot.shared.api.ai.AiLinkMetadataResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/links")
public class AiLinkMetadataController {

  private final AiLinkMetadataService metadataService;

  public AiLinkMetadataController(AiLinkMetadataService metadataService) {
    this.metadataService = metadataService;
  }

  @GetMapping("/{code}/metadata")
  public ResponseEntity<AiLinkMetadataResponse> metadata(@PathVariable("code") String code) {
    return ResponseEntity.ok(metadataService.getByCode(code));
  }

  @PostMapping("/{code}/metadata/regenerate")
  public ResponseEntity<AiLinkMetadataResponse> regenerate(@PathVariable("code") String code) {
    return ResponseEntity.ok(metadataService.regenerate(code));
  }
}
