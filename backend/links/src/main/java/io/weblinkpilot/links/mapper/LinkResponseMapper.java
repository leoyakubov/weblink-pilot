package io.weblinkpilot.links.mapper;

import io.weblinkpilot.links.cache.ShortLinkSnapshot;
import io.weblinkpilot.links.domain.ShortLink;
import io.weblinkpilot.links.support.PublicUrlBuilder;
import io.weblinkpilot.shared.api.ai.AiLinkMetadataResponse;
import io.weblinkpilot.shared.api.links.LinkResponse;
import io.weblinkpilot.shared.ports.LinkOwnerMetadataService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LinkResponseMapper {

  private static final String ANONYMOUS_ROLE = "ANONYMOUS";

  private final PublicUrlBuilder publicUrlBuilder;
  private final LinkOwnerMetadataService linkOwnerMetadataService;

  public LinkResponseMapper(
      PublicUrlBuilder publicUrlBuilder, LinkOwnerMetadataService linkOwnerMetadataService) {
    this.publicUrlBuilder = publicUrlBuilder;
    this.linkOwnerMetadataService = linkOwnerMetadataService;
  }

  public LinkResponse toResponse(ShortLink link) {
    return toResponse(link, null);
  }

  public LinkResponse toResponse(
      ShortLink link, Map<String, AiLinkMetadataResponse> metadataByCode) {
    return new LinkResponse(
        link.getCode(),
        publicUrlBuilder.buildShortUrl(link.getCode()),
        publicUrlBuilder.buildQrCodeUrl(link.getCode()),
        link.getOriginalUrl(),
        link.getCreatedAt(),
        link.getExpiresAt(),
        link.getClickCount(),
        link.getOwnerUsername(),
        roleForOwner(link.getOwnerUsername()),
        metadataFor(link.getCode(), metadataByCode));
  }

  public LinkResponse toResponse(
      ShortLinkSnapshot snapshot, Map<String, AiLinkMetadataResponse> metadataByCode) {
    return new LinkResponse(
        snapshot.code(),
        publicUrlBuilder.buildShortUrl(snapshot.code()),
        publicUrlBuilder.buildQrCodeUrl(snapshot.code()),
        snapshot.originalUrl(),
        snapshot.createdAt(),
        snapshot.expiresAt(),
        snapshot.clickCount(),
        snapshot.ownerUsername(),
        roleForOwner(snapshot.ownerUsername()),
        metadataFor(snapshot.code(), metadataByCode));
  }

  private AiLinkMetadataResponse metadataFor(
      String code, Map<String, AiLinkMetadataResponse> metadataByCode) {
    return metadataByCode == null ? null : metadataByCode.get(code);
  }

  private String roleForOwner(String ownerUsername) {
    return ownerUsername == null
        ? ANONYMOUS_ROLE
        : linkOwnerMetadataService.roleForOwner(ownerUsername);
  }
}
