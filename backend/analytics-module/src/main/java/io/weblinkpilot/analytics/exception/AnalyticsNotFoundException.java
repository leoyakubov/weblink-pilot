package io.weblinkpilot.analytics.exception;

public class AnalyticsNotFoundException extends RuntimeException {
    public AnalyticsNotFoundException(String code) {
        super("Analytics not found for code: " + code);
    }
}
