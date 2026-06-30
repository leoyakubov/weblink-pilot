package io.weblinkpilot.analytics.useragent;

import java.util.Arrays;
import java.util.Locale;

public enum DeviceType {
  MOBILE("Mobile", "iPhone; CPU iPhone OS 17_5 like Mac OS X"),
  TABLET("Tablet", "iPad; CPU OS 17_5 like Mac OS X"),
  DESKTOP("Desktop", "Macintosh; Intel Mac OS X 14_5"),
  UNKNOWN("UNKNOWN", "Macintosh; Intel Mac OS X 14_5");

  private final String seedValue;
  private final String demoPlatform;

  DeviceType(String seedValue, String demoPlatform) {
    this.seedValue = seedValue;
    this.demoPlatform = demoPlatform;
  }

  public String value() {
    return name();
  }

  public String seedValue() {
    return seedValue;
  }

  public String demoPlatform() {
    return demoPlatform;
  }

  public static DeviceType fromSeedValue(String value) {
    if (value == null || value.isBlank()) {
      return DESKTOP;
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    return Arrays.stream(values())
        .filter(type -> type.seedValue.toLowerCase(Locale.ROOT).equals(normalized))
        .findFirst()
        .orElse(DESKTOP);
  }
}
