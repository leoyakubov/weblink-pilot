package io.weblinkpilot.analytics.useragent;

public record UserAgentMetadata(BrowserFamily browserFamily, DeviceType deviceType) {

  public String browserFamilyValue() {
    return browserFamily.value();
  }

  public String deviceTypeValue() {
    return deviceType.value();
  }
}
