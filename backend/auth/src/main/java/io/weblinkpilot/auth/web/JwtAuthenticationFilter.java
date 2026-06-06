package io.weblinkpilot.auth.web;

import io.weblinkpilot.auth.service.JwtService;
import io.weblinkpilot.auth.service.JwtService.JwtClaims;
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

  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = header.substring("Bearer ".length()).trim();
    if (token.isBlank()) {
      if (requiresStrictAuth(request)) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing bearer token");
      } else {
        filterChain.doFilter(request, response);
      }
      return;
    }

    JwtClaims claims = jwtService.parseToken(token).orElse(null);
    if (claims == null) {
      if (requiresStrictAuth(request)) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired bearer token");
      } else {
        filterChain.doFilter(request, response);
      }
      return;
    }

    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            claims.username(), null, List.of(new SimpleGrantedAuthority("ROLE_" + claims.role())));
    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    filterChain.doFilter(request, response);
  }

  private boolean requiresStrictAuth(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/api/v1/auth/me") || path.startsWith("/api/v1/admin/");
  }
}
