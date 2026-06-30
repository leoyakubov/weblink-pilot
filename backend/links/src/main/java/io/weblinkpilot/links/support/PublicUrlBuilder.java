package io.weblinkpilot.links.support;

import io.weblinkpilot.links.config.ShortLinkProperties;
import org.springframework.stereotype.Component;

@Component
public class PublicUrlBuilder {

  private static final String REDIRECT_PATH = "/r/";
  private static final String QR_SCAN_PATH = "/q/";
  private static final String API_URLS_PATH = "/api/v1/urls/";
  private static final String QR_RESOURCE = "/qr";
  private static final String PREVIEW_RESOURCE = "/preview";

  private final String baseUrl;

  public PublicUrlBuilder(ShortLinkProperties properties) {
    this.baseUrl = normalize(properties.getPublicBaseUrl());
  }

  public String buildShortUrl(String code) {
    return baseUrl + REDIRECT_PATH + code;
  }

  public String buildQrCodeUrl(String code) {
    return baseUrl + API_URLS_PATH + code + QR_RESOURCE;
  }

  public String buildQrScanUrl(String code) {
    return baseUrl + QR_SCAN_PATH + code;
  }

  public String buildPreviewUrl(String code) {
    return baseUrl + API_URLS_PATH + code + PREVIEW_RESOURCE;
  }

  private String normalize(String value) {
    String trimmed = value == null ? "" : value.trim();
    return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
  }
}
