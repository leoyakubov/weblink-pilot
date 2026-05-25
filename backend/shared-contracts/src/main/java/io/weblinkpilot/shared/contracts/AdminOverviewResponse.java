package io.weblinkpilot.shared.contracts;

public record AdminOverviewResponse(
    long totalUsers,
    long adminUsers,
    long totalLinks,
    long anonymousLinks,
    long ownedLinks,
    long totalClicks) {}
