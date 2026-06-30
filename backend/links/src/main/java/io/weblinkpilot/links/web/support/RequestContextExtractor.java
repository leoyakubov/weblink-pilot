package io.weblinkpilot.links.web.support;

import io.weblinkpilot.links.support.RedirectRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class RequestContextExtractor {

  private static final String USER_AGENT_HEADER = "User-Agent";
  private static final String REFERER_HEADER = "Referer";
  private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
  private static final String FORWARDED_FOR_SEPARATOR = ",";

  private final CountryResolver countryResolver;

  public RequestContextExtractor(CountryResolver countryResolver) {
    this.countryResolver = countryResolver;
  }

  public RedirectRequestContext extract(HttpServletRequest request) {
    String clientIp = extractClientIp(request);
    return new RedirectRequestContext(
        clientIp,
        request.getHeader(USER_AGENT_HEADER),
        request.getHeader(REFERER_HEADER),
        countryResolver.resolve(request, clientIp));
  }

  private String extractClientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(FORWARDED_FOR_SEPARATOR)[0].trim();
    }
    return request.getRemoteAddr();
  }
}
