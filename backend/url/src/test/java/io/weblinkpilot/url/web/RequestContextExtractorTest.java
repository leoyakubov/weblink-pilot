package io.weblinkpilot.url.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

class RequestContextExtractorTest {

  @Test
  void prefersForwardedForAndCopiesHeaders() {
    CountryResolver countryResolver = Mockito.mock(CountryResolver.class);
    when(countryResolver.resolve(Mockito.any(), Mockito.anyString())).thenReturn("US");
    RequestContextExtractor extractor = new RequestContextExtractor(countryResolver);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Forwarded-For", "203.0.113.10, 198.51.100.2");
    request.addHeader("User-Agent", "Mozilla/5.0");
    request.addHeader("Referer", "https://github.com/weblinkpilot/weblink-pilot/page");
    request.setRemoteAddr("127.0.0.1");

    RedirectRequestContext context = extractor.extract(request);

    assertThat(context.clientIp()).isEqualTo("203.0.113.10");
    assertThat(context.userAgent()).isEqualTo("Mozilla/5.0");
    assertThat(context.referrer()).isEqualTo("https://github.com/weblinkpilot/weblink-pilot/page");
    assertThat(context.country()).isEqualTo("US");
  }

  @Test
  void fallsBackToRemoteAddrWhenForwardedForMissing() {
    CountryResolver countryResolver = Mockito.mock(CountryResolver.class);
    when(countryResolver.resolve(Mockito.any(HttpServletRequest.class), Mockito.anyString()))
        .thenReturn("LOCAL");
    RequestContextExtractor extractor = new RequestContextExtractor(countryResolver);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("127.0.0.1");

    RedirectRequestContext context = extractor.extract(request);

    assertThat(context.clientIp()).isEqualTo("127.0.0.1");
    assertThat(context.country()).isEqualTo("LOCAL");
  }

  @Test
  void ignoresBlankForwardedForAndUsesRemoteAddr() {
    CountryResolver countryResolver = Mockito.mock(CountryResolver.class);
    when(countryResolver.resolve(Mockito.any(HttpServletRequest.class), Mockito.anyString()))
        .thenReturn("UNKNOWN");
    RequestContextExtractor extractor = new RequestContextExtractor(countryResolver);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Forwarded-For", "   ");
    request.setRemoteAddr("198.51.100.77");

    RedirectRequestContext context = extractor.extract(request);

    assertThat(context.clientIp()).isEqualTo("198.51.100.77");
    assertThat(context.country()).isEqualTo("UNKNOWN");
  }
}
