package io.weblinkpilot.auth.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.auth.token.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock private JwtService jwtService;

  @Mock private FilterChain filterChain;

  private JwtAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    filter = new JwtAuthenticationFilter(jwtService);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void passesThroughWhenAuthorizationHeaderIsMissing() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/v1/urls");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void rejectsBlankBearerTokenOnStrictEndpoint() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/v1/auth/me");
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer   ");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    verify(filterChain, never()).doFilter(request, response);
  }

  @Test
  void rejectsInvalidTokenOnStrictEndpoint() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/v1/admin/overview");
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(jwtService.parseToken("invalid-token")).thenReturn(java.util.Optional.empty());

    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    verify(filterChain, never()).doFilter(request, response);
  }

  @Test
  void authenticatesValidToken() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/v1/auth/me");
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(jwtService.parseToken("valid-token"))
        .thenReturn(
            java.util.Optional.of(
                new JwtService.JwtClaims(
                    "alice", "ADMIN", Instant.now(), Instant.now().plusSeconds(60))));

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();
    assertThat(authentication.getName()).isEqualTo("alice");
    assertThat(authentication.getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_ADMIN");
  }
}
