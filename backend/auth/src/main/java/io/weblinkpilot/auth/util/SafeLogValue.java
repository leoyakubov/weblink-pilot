package io.weblinkpilot.auth.support;

import java.util.Locale;

public final class SafeLogValue {

  private SafeLogValue() {}

  public static String email(String value) {
    if (value == null || value.isBlank()) {
      return "blank";
    }

    String normalized = value.trim().toLowerCase(Locale.ROOT);
    int atIndex = normalized.indexOf('@');
    if (atIndex <= 0 || atIndex == normalized.length() - 1) {
      return "masked";
    }

    String localPart = normalized.substring(0, atIndex);
    String domain = normalized.substring(atIndex + 1);
    return maskLocalPart(localPart) + "@" + maskDomain(domain);
  }

  private static String maskLocalPart(String value) {
    if (value.length() == 1) {
      return "*";
    }
    return value.charAt(0) + "***";
  }

  private static String maskDomain(String value) {
    int dotIndex = value.lastIndexOf('.');
    if (dotIndex <= 0 || dotIndex == value.length() - 1) {
      return "***";
    }
    return "***" + value.substring(dotIndex);
  }
}
