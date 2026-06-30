package io.weblinkpilot.shared.api.common;

import java.time.OffsetDateTime;

public record ApiErrorResponse(
    OffsetDateTime timestamp, int status, String error, String code, String message, String path) {}
