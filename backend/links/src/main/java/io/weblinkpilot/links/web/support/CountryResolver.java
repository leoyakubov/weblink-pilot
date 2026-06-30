package io.weblinkpilot.links.web.support;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class CountryResolver {

  private static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";
  private static final String LOCAL_COUNTRY = "LOCAL";
  private static final String UNKNOWN_COUNTRY = "UNKNOWN";
  private static final int MIN_COUNTRY_CODE_LENGTH = 2;
  private static final int MAX_COUNTRY_CODE_LENGTH = 8;
  private static final List<String> TRUSTED_COUNTRY_HEADERS =
      List.of("CF-IPCountry", "CloudFront-Viewer-Country", "X-App-Country", "X-Geo-Country");

  public String resolve(HttpServletRequest request, String clientIp) {
    for (String header : TRUSTED_COUNTRY_HEADERS) {
      String country = normalizeCountryCode(request.getHeader(header));
      if (country != null) {
        return country;
      }
    }

    String countryFromLocale = countryFromAcceptLanguage(request.getHeader(ACCEPT_LANGUAGE_HEADER));
    if (countryFromLocale != null) {
      return countryFromLocale;
    }

    if (isLocalOrPrivateAddress(clientIp)) {
      return LOCAL_COUNTRY;
    }

    return UNKNOWN_COUNTRY;
  }

  private String normalizeCountryCode(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    String normalized = value.trim().toUpperCase(Locale.ROOT);
    return normalized.length() >= MIN_COUNTRY_CODE_LENGTH
            && normalized.length() <= MAX_COUNTRY_CODE_LENGTH
        ? normalized
        : null;
  }

  private String countryFromAcceptLanguage(String acceptLanguage) {
    if (acceptLanguage == null || acceptLanguage.isBlank()) {
      return null;
    }

    try {
      for (Locale.LanguageRange range : Locale.LanguageRange.parse(acceptLanguage)) {
        Locale locale = Locale.forLanguageTag(range.getRange());
        if (!locale.getCountry().isBlank()) {
          return locale.getCountry().toUpperCase(Locale.ROOT);
        }
      }
    } catch (IllegalArgumentException ignored) {
      // fall through to LOCAL/UNKNOWN handling
    }

    return null;
  }

  private boolean isLocalOrPrivateAddress(String clientIp) {
    if (clientIp == null || clientIp.isBlank()) {
      return false;
    }

    try {
      InetAddress address = InetAddress.getByName(clientIp.trim());
      return address.isAnyLocalAddress()
          || address.isLoopbackAddress()
          || address.isSiteLocalAddress()
          || address.isLinkLocalAddress();
    } catch (UnknownHostException ex) {
      return false;
    }
  }
}
