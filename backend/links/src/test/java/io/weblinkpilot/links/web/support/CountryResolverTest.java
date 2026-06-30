package io.weblinkpilot.links.web.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class CountryResolverTest {

  private final CountryResolver resolver = new CountryResolver();

  @Test
  void prefersTrustedCountryHeaders() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("CF-IPCountry", "de");

    assertThat(resolver.resolve(request, "198.51.100.10")).isEqualTo("DE");
  }

  @Test
  void skipsBlankTrustedHeaderAndUsesNextTrustedHeader() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("CF-IPCountry", " ");
    request.addHeader("CloudFront-Viewer-Country", "fr");

    assertThat(resolver.resolve(request, "198.51.100.10")).isEqualTo("FR");
  }

  @Test
  void fallsBackToAcceptLanguageCountry() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Accept-Language", "en-US,en;q=0.9");

    assertThat(resolver.resolve(request, "198.51.100.10")).isEqualTo("US");
  }

  @Test
  void fallsBackToLocalForPrivateAddressWhenNoCountrySignalsExist() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Accept-Language", "not-a-real-value");

    assertThat(resolver.resolve(request, "10.0.0.1")).isEqualTo("LOCAL");
  }

  @Test
  void marksLocalLoopbackAsLocalWhenNoCountrySignalsExist() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    assertThat(resolver.resolve(request, "127.0.0.1")).isEqualTo("LOCAL");
  }

  @Test
  void marksPublicIpAsUnknownWhenNoCountrySignalsExist() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    assertThat(resolver.resolve(request, "198.51.100.10")).isEqualTo("UNKNOWN");
  }
}
