package io.weblinkpilot.links.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class RequestContextExtractor {

  private final CountryResolver countryResolver;

  public RequestContextExtractor(CountryResolver countryResolver) {
    this.countryResolver = countryResolver;
  }

  public RedirectRequestContext extract(HttpServletRequest request) {
    String clientIp = extractClientIp(request);
    return new RedirectRequestContext(
        clientIp,
        request.getHeader("User-Agent"),
        request.getHeader("Referer"),
        countryResolver.resolve(request, clientIp));
  }

  private String extractClientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
