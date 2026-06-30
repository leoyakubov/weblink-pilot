package io.weblinkpilot.analytics.useragent;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class UserAgentParser {

  public UserAgentMetadata parse(String userAgent) {
    if (userAgent == null || userAgent.isBlank()) {
      return new UserAgentMetadata(BrowserFamily.UNKNOWN, DeviceType.UNKNOWN);
    }

    String normalized = userAgent.toLowerCase(Locale.ROOT);
    BrowserFamily browser = detectBrowser(normalized);
    DeviceType device = detectDevice(normalized);
    return new UserAgentMetadata(browser, device);
  }

  private BrowserFamily detectBrowser(String userAgent) {
    if (userAgent.contains("edg/") || userAgent.contains("edge")) {
      return BrowserFamily.EDGE;
    }
    if (userAgent.contains("firefox/")) {
      return BrowserFamily.FIREFOX;
    }
    if (userAgent.contains("chrome/") && !userAgent.contains("edg/")) {
      return BrowserFamily.CHROME;
    }
    if (userAgent.contains("safari/") && !userAgent.contains("chrome/")) {
      return BrowserFamily.SAFARI;
    }
    return BrowserFamily.UNKNOWN;
  }

  private DeviceType detectDevice(String userAgent) {
    if (userAgent.contains("mobile")
        || userAgent.contains("android")
        || userAgent.contains("iphone")) {
      return DeviceType.MOBILE;
    }
    if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
      return DeviceType.TABLET;
    }
    return DeviceType.DESKTOP;
  }
}
