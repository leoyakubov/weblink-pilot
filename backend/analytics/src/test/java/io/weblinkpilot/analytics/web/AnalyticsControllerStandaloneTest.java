package io.weblinkpilot.analytics.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.weblinkpilot.analytics.service.AnalyticsQueryService;
import io.weblinkpilot.shared.contracts.AnalyticsCountryStatResponse;
import io.weblinkpilot.shared.contracts.AnalyticsSummaryResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerStandaloneTest {

    @Mock
    private AnalyticsQueryService analyticsQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AnalyticsController controller = new AnalyticsController(analyticsQueryService, new SimpleMeterRegistry());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void returnsClickCount() throws Exception {
        when(analyticsQueryService.countClicks("demo")).thenReturn(12L);

        mockMvc.perform(get("/api/v1/analytics/demo/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(12));
    }

    @Test
    void returnsSummary() throws Exception {
        AnalyticsSummaryResponse summary = new AnalyticsSummaryResponse(
                "demo",
                12L,
                5L,
                OffsetDateTime.now(ZoneOffset.UTC),
                "https://github.com",
                "Chrome",
                "Desktop",
                List.of(new AnalyticsCountryStatResponse("US", 3L))
        );
        when(analyticsQueryService.summarize("demo")).thenReturn(summary);

        mockMvc.perform(get("/api/v1/analytics/demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("demo"))
                .andExpect(jsonPath("$.totalClicks").value(12))
                .andExpect(jsonPath("$.topCountries[0].country").value("US"));
    }
}
