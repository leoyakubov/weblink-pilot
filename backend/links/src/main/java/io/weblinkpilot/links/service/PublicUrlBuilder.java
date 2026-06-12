package io.weblinkpilot.links.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PublicUrlBuilder {

  private final String baseUrl;

  public PublicUrlBuilder(@Value("${app.public-base-url}") String baseUrl) {
    this.baseUrl = normalize(baseUrl);
  }

  public String buildShortUrl(String code) {
    return baseUrl + "/r/" + code;
  }

  public String buildQrCodeUrl(String code) {
    return baseUrl + "/api/v1/urls/" + code + "/qr";
  }

  public String buildQrScanUrl(String code) {
    return baseUrl + "/q/" + code;
  }

  public String buildPreviewUrl(String code) {
    return baseUrl + "/api/v1/urls/" + code + "/preview";
  }

  private String normalize(String value) {
    String trimmed = value == null ? "" : value.trim();
    return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
  }
}
