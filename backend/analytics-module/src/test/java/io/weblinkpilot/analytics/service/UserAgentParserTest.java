package io.weblinkpilot.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserAgentParserTest {

    private final UserAgentParser parser = new UserAgentParser();

    @Test
    void parsesCommonUserAgents() {
        assertThat(parser.parse("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/123.0"))
                .isEqualTo(new UserAgentMetadata("CHROME", "DESKTOP"));
    }
}
