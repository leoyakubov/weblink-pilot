package io.weblinkpilot.ai.repository;

import io.weblinkpilot.ai.domain.AiLinkMetadata;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiLinkMetadataRepository extends JpaRepository<AiLinkMetadata, Long> {

  Optional<AiLinkMetadata> findByShortCode(String shortCode);

  boolean existsByShortCode(String shortCode);
}
