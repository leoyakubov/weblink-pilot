package io.weblinkpilot.links.web;

public record RedirectRequestContext(
    String clientIp, String userAgent, String referrer, String country) {}
