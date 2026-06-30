package io.weblinkpilot.platform.observability;

public enum BusinessMetric {
  LINKS_CREATED("weblinkpilot.links.created.events", "Total number of short links created"),
  LINKS_CLICKED("weblinkpilot.links.clicked.events", "Total number of short link redirects");

  private final String meterName;
  private final String description;

  BusinessMetric(String meterName, String description) {
    this.meterName = meterName;
    this.description = description;
  }

  public String meterName() {
    return meterName;
  }

  public String description() {
    return description;
  }
}
