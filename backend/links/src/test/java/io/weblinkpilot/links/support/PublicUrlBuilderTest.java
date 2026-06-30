package io.weblinkpilot.links.support;

import static org.assertj.core.api.Assertions.assertThat;

import io.weblinkpilot.links.config.ShortLinkProperties;
import org.junit.jupiter.api.Test;

class PublicUrlBuilderTest {

  @Test
  void trimsTrailingSlashFromBaseUrl() {
    PublicUrlBuilder builder = new PublicUrlBuilder(properties("http://localhost:8080/"));

    assertThat(builder.buildShortUrl("abc123")).isEqualTo("http://localhost:8080/r/abc123");
    assertThat(builder.buildQrCodeUrl("abc123"))
        .isEqualTo("http://localhost:8080/api/v1/urls/abc123/qr");
    assertThat(builder.buildQrScanUrl("abc123")).isEqualTo("http://localhost:8080/q/abc123");
    assertThat(builder.buildPreviewUrl("abc123"))
        .isEqualTo("http://localhost:8080/api/v1/urls/abc123/preview");
  }

  @Test
  void preservesBaseUrlWithoutTrailingSlash() {
    PublicUrlBuilder builder =
        new PublicUrlBuilder(properties("https://github.com/weblinkpilot/weblink-pilot"));

    assertThat(builder.buildShortUrl("demo"))
        .isEqualTo("https://github.com/weblinkpilot/weblink-pilot/r/demo");
  }

  @Test
  void toleratesMissingBaseUrl() {
    PublicUrlBuilder builder = new PublicUrlBuilder(properties(null));

    assertThat(builder.buildShortUrl("demo")).isEqualTo("/r/demo");
  }

  private static ShortLinkProperties properties(String baseUrl) {
    ShortLinkProperties properties = new ShortLinkProperties();
    properties.setPublicBaseUrl(baseUrl);
    return properties;
  }
}
