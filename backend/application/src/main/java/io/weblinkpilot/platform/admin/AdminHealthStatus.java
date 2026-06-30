package io.weblinkpilot.platform.admin;

enum AdminHealthStatus {
  UP,
  WARNING,
  DOWN,
  INFO,
  UNKNOWN,
  DISABLED,
  CONFIGURED;

  String value() {
    return name();
  }
}
