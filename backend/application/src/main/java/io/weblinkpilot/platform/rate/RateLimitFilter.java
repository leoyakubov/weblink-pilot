package io.weblinkpilot.platform.rate;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.weblinkpilot.platform.web.ApiRoutes;
import io.weblinkpilot.platform.web.PlatformHttpHeaders;
import io.weblinkpilot.shared.api.common.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
  private static final String ERROR_CODE_RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
  private static final String RATE_LIMIT_MESSAGE = "Too many requests. Please retry later.";
  private static final String UNKNOWN_CLIENT = "unknown";
  private static final String MASKED_CLIENT = "masked";

  private final RateLimitService rateLimitService;
  private final ObjectMapper objectMapper;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring singleton filter stores injected service and mapper collaborators.")
  public RateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper) {
    this.rateLimitService = rateLimitService;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (HttpMethodName.OPTIONS.matches(request.getMethod())) {
      return true;
    }

    String path = request.getRequestURI();
    return path == null
        || (!path.startsWith(ApiRoutes.API_V1_PREFIX)
            && !path.startsWith(ApiRoutes.REDIRECT_PREFIX)
            && !path.startsWith(ApiRoutes.QR_REDIRECT_PREFIX));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    String clientIp = clientIp(request);
    RateLimitDecision decision = rateLimitService.tryConsume(path, clientIp);
    response.setHeader(PlatformHttpHeaders.X_RATE_LIMIT_LIMIT, String.valueOf(decision.limit()));
    response.setHeader(
        PlatformHttpHeaders.X_RATE_LIMIT_REMAINING, String.valueOf(decision.remaining()));

    if (!decision.allowed()) {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      ApiErrorResponse body =
          new ApiErrorResponse(
              OffsetDateTime.now(ZoneOffset.UTC),
              HttpStatus.TOO_MANY_REQUESTS.value(),
              HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
              ERROR_CODE_RATE_LIMIT_EXCEEDED,
              RATE_LIMIT_MESSAGE,
              path);
      log.warn(
          "rate.limit.exceeded path={} clientIp={} retryAfterSeconds={}",
          path,
          maskIp(clientIp),
          decision.retryAfterSeconds());
      objectMapper.writeValue(response.getWriter(), body);
      return;
    }

    filterChain.doFilter(request, response);
  }

  private String clientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader(PlatformHttpHeaders.X_FORWARDED_FOR);
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private String maskIp(String ipAddress) {
    if (ipAddress == null || ipAddress.isBlank()) {
      return UNKNOWN_CLIENT;
    }
    if (ipAddress.contains(".")) {
      String[] parts = ipAddress.split("\\.");
      if (parts.length == 4) {
        return parts[0] + "." + parts[1] + "." + parts[2] + ".***";
      }
    }
    if (ipAddress.contains(":")) {
      int index = ipAddress.indexOf(':');
      return index > 0 ? ipAddress.substring(0, index + 1) + "****" : "****";
    }
    return MASKED_CLIENT;
  }

  private enum HttpMethodName {
    OPTIONS;

    private boolean matches(String method) {
      return name().equalsIgnoreCase(method);
    }
  }
}
