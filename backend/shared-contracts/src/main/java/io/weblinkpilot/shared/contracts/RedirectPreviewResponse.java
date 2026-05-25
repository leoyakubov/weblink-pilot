package io.weblinkpilot.shared.contracts;

public record RedirectPreviewResponse(
    String code, String shortUrl, String targetUrl, int status, String locationHeader) {}
