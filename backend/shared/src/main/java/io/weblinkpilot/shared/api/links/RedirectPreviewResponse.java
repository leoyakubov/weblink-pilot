package io.weblinkpilot.shared.api.links;

public record RedirectPreviewResponse(
    String code, String shortUrl, String targetUrl, int status, String locationHeader) {}
