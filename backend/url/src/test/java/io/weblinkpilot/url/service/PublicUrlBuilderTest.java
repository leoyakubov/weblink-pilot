package io.weblinkpilot.url.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PublicUrlBuilderTest {

    @Test
    void trimsTrailingSlashFromBaseUrl() {
        PublicUrlBuilder builder = new PublicUrlBuilder("http://localhost:8080/");

        assertThat(builder.buildShortUrl("abc123")).isEqualTo("http://localhost:8080/r/abc123");
        assertThat(builder.buildQrCodeUrl("abc123")).isEqualTo("http://localhost:8080/api/v1/urls/abc123/qr");
        assertThat(builder.buildQrScanUrl("abc123")).isEqualTo("http://localhost:8080/q/abc123");
        assertThat(builder.buildPreviewUrl("abc123")).isEqualTo("http://localhost:8080/api/v1/urls/abc123/preview");
    }

    @Test
    void preservesBaseUrlWithoutTrailingSlash() {
        PublicUrlBuilder builder = new PublicUrlBuilder("https://example.com");

        assertThat(builder.buildShortUrl("demo")).isEqualTo("https://example.com/r/demo");
    }

    @Test
    void toleratesMissingBaseUrl() {
        PublicUrlBuilder builder = new PublicUrlBuilder(null);

        assertThat(builder.buildShortUrl("demo")).isEqualTo("/r/demo");
    }
}
