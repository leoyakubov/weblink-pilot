package io.weblinkpilot.analytics.bootstrap;

import io.weblinkpilot.analytics.useragent.BrowserFamily;
import io.weblinkpilot.analytics.useragent.DeviceType;

final class DemoUserAgent {

  private static final String APPLE_WEBKIT_VERSION = "537.36";
  private static final String DEMO_BROWSER_VERSION = "125.0";
  private static final String USER_AGENT_TEMPLATE =
      "Mozilla/5.0 (%s) AppleWebKit/%s (KHTML, like Gecko) %s/%s Safari/%s";

  private DemoUserAgent() {}

  static String forBrowserAndDevice(String browserFamily, String deviceType) {
    BrowserFamily browser = BrowserFamily.fromSeedValue(browserFamily);
    return USER_AGENT_TEMPLATE.formatted(
        DeviceType.fromSeedValue(deviceType).demoPlatform(),
        APPLE_WEBKIT_VERSION,
        browser.seedValue(),
        DEMO_BROWSER_VERSION,
        APPLE_WEBKIT_VERSION);
  }
}
