package io.weblinkpilot.analytics.web;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.weblinkpilot.analytics.service.AnalyticsQueryService;
import io.weblinkpilot.shared.contracts.AnalyticsCountryStatResponse;
import io.weblinkpilot.shared.contracts.AnalyticsSummaryResponse;
import io.weblinkpilot.shared.contracts.LinkResponse;
import io.weblinkpilot.url.service.UrlLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerStandaloneTest {

    @Mock
    private AnalyticsQueryService analyticsQueryService;

    @Mock
    private UrlLookupService urlLookupService;

    private AnalyticsController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new AnalyticsController(analyticsQueryService, urlLookupService, new SimpleMeterRegistry());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void returnsClickCount() throws Exception {
        when(urlLookupService.getByCode("demo")).thenReturn(new LinkResponse(
                "demo",
                "http://localhost:8080/r/demo",
                "http://localhost:8080/api/v1/urls/demo/qr",
                "https://github.com/weblinkpilot/weblink-pilot",
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                0L,
                null
        ));
        when(analyticsQueryService.countClicks("demo")).thenReturn(12L);

        mockMvc.perform(get("/api/v1/analytics/demo/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(12));
    }

    @Test
    void returnsSummary() throws Exception {
        when(urlLookupService.getByCode("demo")).thenReturn(new LinkResponse(
                "demo",
                "http://localhost:8080/r/demo",
                "http://localhost:8080/api/v1/urls/demo/qr",
                "https://github.com/weblinkpilot/weblink-pilot",
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                0L,
                null
        ));
        AnalyticsSummaryResponse summary = new AnalyticsSummaryResponse(
                "demo",
                12L,
                9L,
                3L,
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
                .andExpect(jsonPath("$.redirectClicks").value(9))
                .andExpect(jsonPath("$.qrScans").value(3))
                .andExpect(jsonPath("$.topCountries[0].country").value("US"));
    }

    @Test
    void allowsAnonymousAccessForPublicLinks() {
        when(urlLookupService.getByCode("public")).thenReturn(new LinkResponse(
                "public",
                "http://localhost:8080/r/public",
                "http://localhost:8080/api/v1/urls/public/qr",
                "https://github.com/weblinkpilot/weblink-pilot",
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                0L,
                null
        ));
        when(analyticsQueryService.countClicks("public")).thenReturn(1L);

        long count = controller.count(null, "public");

        assertThat(count).isEqualTo(1L);
    }

    @Test
    void rejectsAnonymousAccessForOwnedLinks() {
        when(urlLookupService.getByCode("demo")).thenReturn(new LinkResponse(
                "demo",
                "http://localhost:8080/r/demo",
                "http://localhost:8080/api/v1/urls/demo/qr",
                "https://github.com/weblinkpilot/weblink-pilot",
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                0L,
                "owner"
        ));

        assertThatThrownBy(() -> controller.summary(null, "demo"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Sign in to view analytics for this link");
    }

    @Test
    void rejectsDifferentUsersForOwnedLinks() {
        when(urlLookupService.getByCode("demo")).thenReturn(new LinkResponse(
                "demo",
                "http://localhost:8080/r/demo",
                "http://localhost:8080/api/v1/urls/demo/qr",
                "https://github.com/weblinkpilot/weblink-pilot",
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                0L,
                "owner"
        ));
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("someone-else");
        Collection<? extends GrantedAuthority> userAuthorities = List.of((GrantedAuthority) () -> "ROLE_USER");
        doReturn(userAuthorities).when(authentication).getAuthorities();

        assertThatThrownBy(() -> controller.count(authentication, "demo"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("You can only view analytics for your own links");
    }

    @Test
    void allowsAdminAccessForOwnedLinks() {
        when(urlLookupService.getByCode("demo")).thenReturn(new LinkResponse(
                "demo",
                "http://localhost:8080/r/demo",
                "http://localhost:8080/api/v1/urls/demo/qr",
                "https://github.com/weblinkpilot/weblink-pilot",
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                0L,
                "owner"
        ));
        when(analyticsQueryService.countClicks("demo")).thenReturn(12L);
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        Collection<? extends GrantedAuthority> adminAuthorities = List.of((GrantedAuthority) () -> "ROLE_ADMIN");
        doReturn(adminAuthorities).when(authentication).getAuthorities();

        long count = controller.count(authentication, "demo");

        assertThat(count).isEqualTo(12L);
    }
}
