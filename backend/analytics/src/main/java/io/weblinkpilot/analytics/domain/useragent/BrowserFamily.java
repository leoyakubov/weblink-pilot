package io.weblinkpilot.analytics.useragent;

import java.util.Arrays;
import java.util.Locale;

public enum BrowserFamily {
  CHROME("Chrome"),
  SAFARI("Safari"),
  FIREFOX("Firefox"),
  EDGE("Edge"),
  UNKNOWN("UNKNOWN");

  private final String seedValue;

  BrowserFamily(String seedValue) {
    this.seedValue = seedValue;
  }

  public String value() {
    return name();
  }

  public String seedValue() {
    return seedValue;
  }

  public static BrowserFamily fromSeedValue(String value) {
    if (value == null || value.isBlank()) {
      return UNKNOWN;
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    return Arrays.stream(values())
        .filter(browser -> browser.seedValue.toLowerCase(Locale.ROOT).equals(normalized))
        .findFirst()
        .orElse(UNKNOWN);
  }
}
