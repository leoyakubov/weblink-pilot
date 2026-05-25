package io.weblinkpilot.analytics.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.weblinkpilot.analytics.domain.ClickEvent;
import io.weblinkpilot.analytics.repository.ClickEventRepository;
import io.weblinkpilot.auth.config.BootstrapDefaults;
import io.weblinkpilot.shared.contracts.LinkTrackingSource;
import io.weblinkpilot.url.domain.ShortLink;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import io.weblinkpilot.url.service.UrlCacheService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.hamcrest.Matchers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AnalyticsApiIntegrationTest {

    private static final String AUTH_USER = BootstrapDefaults.ADMIN_USERNAME;
    private static final String AUTH_PASSWORD = BootstrapDefaults.ADMIN_PASSWORD;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ClickEventRepository clickEventRepository;

    @Autowired
    private ShortLinkRepository shortLinkRepository;

    @Autowired
    private UrlCacheService urlCacheService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        clickEventRepository.deleteAllInBatch();
        shortLinkRepository.deleteAllInBatch();

        OffsetDateTime createdAt = OffsetDateTime.of(2026, 5, 22, 11, 45, 0, 0, ZoneOffset.UTC);
        shortLinkRepository.save(new ShortLink(
                "demo-it",
                "https://github.com/weblinkpilot/weblink-pilot",
                null,
                null,
                createdAt,
                null
        ));
        urlCacheService.findByCode("demo-it");

        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void returnsAnalyticsSummaryForClickHistory() throws Exception {
        OffsetDateTime first = OffsetDateTime.of(2026, 5, 22, 12, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime second = first.plusMinutes(2);
        OffsetDateTime third = first.plusMinutes(5);

        clickEventRepository.saveAll(List.of(
                new ClickEvent(
                        "demo-it",
                        first,
                        LinkTrackingSource.REDIRECT,
                        "203.0.113.10",
                        "Mozilla/5.0 Chrome/123.0",
                        "https://news.ycombinator.com",
                        "US",
                        "CHROME",
                        "DESKTOP"
                ),
                new ClickEvent(
                        "demo-it",
                        second,
                        LinkTrackingSource.REDIRECT,
                        "203.0.113.11",
                        "Mozilla/5.0 Safari/17.0",
                        "https://github.com",
                        "DE",
                        "SAFARI",
                        "MOBILE"
                ),
                new ClickEvent(
                        "demo-it",
                        third,
                        LinkTrackingSource.QR_SCAN,
                        "203.0.113.10",
                        "Mozilla/5.0 Firefox/124.0",
                        null,
                        "US",
                        "FIREFOX",
                        "MOBILE"
                )
        ));

        mockMvc.perform(get("/api/v1/analytics/demo-it")
                        .with(httpBasic(AUTH_USER, AUTH_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("demo-it"))
                .andExpect(jsonPath("$.totalClicks").value(3))
                .andExpect(jsonPath("$.redirectClicks").value(2))
                .andExpect(jsonPath("$.qrScans").value(1))
                .andExpect(jsonPath("$.uniqueVisitors").value(2))
                .andExpect(jsonPath("$.lastClickedAt").value("2026-05-22T12:05:00Z"))
                .andExpect(jsonPath("$.lastReferrer").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.lastBrowserFamily").value("FIREFOX"))
                .andExpect(jsonPath("$.lastDeviceType").value("MOBILE"))
                .andExpect(jsonPath("$.topCountries[0].country").value("US"))
                .andExpect(jsonPath("$.topCountries[0].clicks").value(2))
                .andExpect(jsonPath("$.topCountries[1].country").value("DE"))
                .andExpect(jsonPath("$.topCountries[1].clicks").value(1));

        mockMvc.perform(get("/api/v1/analytics/demo-it/count")
                        .with(httpBasic(AUTH_USER, AUTH_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));
    }
}
