package io.weblinkpilot.shared.api.admin;

public record AdminRuntimeMetricResponse(
    String group, String name, String value, String unit, String description) {}
