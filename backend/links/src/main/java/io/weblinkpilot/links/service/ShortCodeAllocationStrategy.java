package io.weblinkpilot.links.service;

import io.weblinkpilot.links.codegen.ShortCodeGenerator;
import io.weblinkpilot.links.config.ShortLinkProperties;
import io.weblinkpilot.links.domain.ShortLink;
import io.weblinkpilot.links.repository.ShortLinkRepository;
import java.time.OffsetDateTime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ShortCodeAllocationStrategy {

  private final ShortLinkRepository repository;
  private final ShortCodeGenerator shortCodeGenerator;
  private final int codeLength;
  private final int maxGenerationAttempts;

  public ShortCodeAllocationStrategy(
      ShortLinkRepository repository,
      ShortCodeGenerator shortCodeGenerator,
      ShortLinkProperties properties) {
    this.repository = repository;
    this.shortCodeGenerator = shortCodeGenerator;
    this.codeLength = properties.getCode().getLength();
    this.maxGenerationAttempts = properties.getCode().getMaxGenerationAttempts();
  }

  public ShortLink createGeneratedLink(
      String normalizedUrl, String ownerUsername, OffsetDateTime now, OffsetDateTime expiresAt) {
    for (int attempt = 1; attempt <= maxGenerationAttempts; attempt++) {
      String code = shortCodeGenerator.generate(codeLength);
      if (repository.existsByCode(code)) {
        continue;
      }

      try {
        return repository.saveAndFlush(
            ShortLink.builder()
                .code(code)
                .originalUrl(normalizedUrl)
                .ownerUsername(ownerUsername)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build());
      } catch (DataIntegrityViolationException exception) {
        if (attempt == maxGenerationAttempts) {
          throw exception;
        }
      }
    }

    throw new IllegalStateException("Unable to generate a unique short code");
  }
}
