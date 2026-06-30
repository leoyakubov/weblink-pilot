package io.weblinkpilot.shared.api.admin;

public record AdminOverviewResponse(
    long totalUsers,
    long adminUsers,
    long totalLinks,
    long anonymousLinks,
    long ownedLinks,
    long totalClicks) {}
