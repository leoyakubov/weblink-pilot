package io.weblinkpilot.config.rate;

import io.weblinkpilot.shared.contracts.ApiErrorResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        return path == null || (!path.startsWith("/api/v1/") && !path.startsWith("/r/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        RateLimitDecision decision = rateLimitService.tryConsume(request.getRequestURI(), clientIp(request));
        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));

        if (!decision.allowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiErrorResponse body = new ApiErrorResponse(
                    OffsetDateTime.now(ZoneOffset.UTC),
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                    "RATE_LIMIT_EXCEEDED",
                    "Too many requests. Please retry later.",
                    request.getRequestURI()
            );
            log.warn("rate.limit.exceeded path={} clientIp={} retryAfterSeconds={}",
                    request.getRequestURI(), maskIp(clientIp(request)), decision.retryAfterSeconds());
            response.getWriter().write(toJson(body));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String maskIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return "unknown";
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
        return "masked";
    }

    private String toJson(ApiErrorResponse body) {
        return "{"
                + "\"timestamp\":\"" + escape(body.timestamp().toString()) + "\","
                + "\"status\":" + body.status() + ","
                + "\"error\":\"" + escape(body.error()) + "\","
                + "\"code\":\"" + escape(body.code()) + "\","
                + "\"message\":\"" + escape(body.message()) + "\","
                + "\"path\":\"" + escape(body.path()) + "\""
                + "}";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
