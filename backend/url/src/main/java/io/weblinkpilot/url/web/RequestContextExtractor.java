package io.weblinkpilot.url.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class RequestContextExtractor {

    public RedirectRequestContext extract(HttpServletRequest request) {
        return new RedirectRequestContext(
                extractClientIp(request),
                request.getHeader("User-Agent"),
                request.getHeader("Referer")
        );
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
