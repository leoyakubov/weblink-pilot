package io.weblinkpilot.analytics.service;

import org.springframework.stereotype.Component;

@Component
public class UserAgentParser {

    public UserAgentMetadata parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return new UserAgentMetadata("UNKNOWN", "UNKNOWN");
        }

        String normalized = userAgent.toLowerCase();
        String browser = detectBrowser(normalized);
        String device = detectDevice(normalized);
        return new UserAgentMetadata(browser, device);
    }

    private String detectBrowser(String userAgent) {
        if (userAgent.contains("edg/") || userAgent.contains("edge")) {
            return "EDGE";
        }
        if (userAgent.contains("firefox/")) {
            return "FIREFOX";
        }
        if (userAgent.contains("chrome/") && !userAgent.contains("edg/")) {
            return "CHROME";
        }
        if (userAgent.contains("safari/") && !userAgent.contains("chrome/")) {
            return "SAFARI";
        }
        return "UNKNOWN";
    }

    private String detectDevice(String userAgent) {
        if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
            return "MOBILE";
        }
        if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
            return "TABLET";
        }
        return "DESKTOP";
    }
}
