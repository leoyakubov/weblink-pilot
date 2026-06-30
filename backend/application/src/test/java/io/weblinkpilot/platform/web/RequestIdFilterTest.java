package io.weblinkpilot.platform.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

  @Test
  void generatesRequestIdWhenHeaderMissing() throws ServletException, IOException {
    RequestIdFilter filter = new RequestIdFilter();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/urls");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, simpleChain());

    assertThat(response.getHeader("X-Request-Id")).isNotBlank();
    assertThat(MDC.get("requestId")).isNull();
  }

  @Test
  void preservesProvidedRequestId() throws ServletException, IOException {
    RequestIdFilter filter = new RequestIdFilter();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/urls");
    request.addHeader("X-Request-Id", "request-123");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, simpleChain());

    assertThat(response.getHeader("X-Request-Id")).isEqualTo("request-123");
    assertThat(MDC.get("requestId")).isNull();
  }

  private FilterChain simpleChain() {
    return (request, response) -> assertThat(MDC.get("requestId")).isNotBlank();
  }
}
