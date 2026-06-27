package io.weblinkpilot.shared.contracts;

public record AdminRuntimeMetricResponse(
    String group, String name, String value, String unit, String description) {}
