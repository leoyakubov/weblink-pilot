package io.weblinkpilot.auth.web.security;

import io.weblinkpilot.auth.token.JwtService;
import io.weblinkpilot.auth.token.JwtService.JwtClaims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";
  private static final String ROLE_PREFIX = "ROLE_";
  private static final String AUTH_ME_PATH = "/api/v1/auth/me";
  private static final String AUTH_ACCOUNT_PATH = "/api/v1/auth/account";
  private static final String ADMIN_PATH = "/api/v1/admin/";
  private static final String MISSING_BEARER_TOKEN_MESSAGE = "Missing bearer token";
  private static final String INVALID_BEARER_TOKEN_MESSAGE = "Invalid or expired bearer token";

  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith(BEARER_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = header.substring(BEARER_PREFIX.length()).trim();
    if (token.isBlank()) {
      if (requiresStrictAuth(request)) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, MISSING_BEARER_TOKEN_MESSAGE);
      } else {
        filterChain.doFilter(request, response);
      }
      return;
    }

    JwtClaims claims = jwtService.parseToken(token).orElse(null);
    if (claims == null) {
      if (requiresStrictAuth(request)) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, INVALID_BEARER_TOKEN_MESSAGE);
      } else {
        filterChain.doFilter(request, response);
      }
      return;
    }

    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            claims.username(),
            null,
            List.of(new SimpleGrantedAuthority(ROLE_PREFIX + claims.role())));
    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    filterChain.doFilter(request, response);
  }

  private boolean requiresStrictAuth(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith(AUTH_ME_PATH)
        || path.startsWith(AUTH_ACCOUNT_PATH)
        || path.startsWith(ADMIN_PATH);
  }
}
