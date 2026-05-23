package io.weblinkpilot.url.web;

public record RedirectRequestContext(
        String clientIp,
        String userAgent,
        String referrer
) {
}
