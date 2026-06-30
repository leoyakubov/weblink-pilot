package io.weblinkpilot.links.support;

public record RedirectRequestContext(
    String clientIp, String userAgent, String referrer, String country) {}
