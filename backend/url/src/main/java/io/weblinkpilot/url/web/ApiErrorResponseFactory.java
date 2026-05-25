package io.weblinkpilot.url.web;

import io.weblinkpilot.shared.contracts.ApiErrorResponse;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;

public final class ApiErrorResponseFactory {

    private ApiErrorResponseFactory() {
    }

    public static ApiErrorResponse create(HttpStatus status, String code, String message, String path) {
        return new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                path
        );
    }
}
