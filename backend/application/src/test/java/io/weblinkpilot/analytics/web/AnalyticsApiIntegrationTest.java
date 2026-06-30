package io.weblinkpilot.analytics.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.weblinkpilot.analytics.service.AnalyticsQueryService;
import io.weblinkpilot.shared.api.analytics.AnalyticsCountryStatResponse;
import io.weblinkpilot.shared.api.analytics.AnalyticsSummaryResponse;
import io.weblinkpilot.shared.ports.LinkOwnershipLookupService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AnalyticsApiIntegrationTest {

  @Mock private AnalyticsQueryService analyticsQueryService;

  @Mock private LinkOwnershipLookupService linkOwnershipLookupService;

  private AnalyticsController controller;

  @BeforeEach
  void setUp() {
    controller =
        new AnalyticsController(
            analyticsQueryService, linkOwnershipLookupService, new SimpleMeterRegistry());
  }

  @Test
  void returnsClickCount() {
    when(linkOwnershipLookupService.ownerUsernameForCode("demo")).thenReturn("owner");
    when(analyticsQueryService.countClicks("demo")).thenReturn(12L);

    assertEquals(12L, controller.count(auth("owner", "USER"), "demo"));
  }

  @Test
  void returnsSummary() {
    when(linkOwnershipLookupService.ownerUsernameForCode("demo")).thenReturn("owner");
    AnalyticsSummaryResponse summary =
        new AnalyticsSummaryResponse(
            "demo",
            12L,
            9L,
            3L,
            5L,
            OffsetDateTime.now(ZoneOffset.UTC),
            "https://github.com",
            "CHROME",
            "DESKTOP",
            List.of(new AnalyticsCountryStatResponse("US", 3L)));
    when(analyticsQueryService.summarize("demo")).thenReturn(summary);

    AnalyticsSummaryResponse response = controller.summary(auth("owner", "USER"), "demo");

    assertEquals("demo", response.code());
    assertEquals(12L, response.totalClicks());
    assertEquals(9L, response.redirectClicks());
    assertEquals(3L, response.qrScans());
    assertEquals("US", response.topCountries().get(0).country());
  }

  @Test
  void allowsAnonymousAccessForPublicLinks() {
    when(linkOwnershipLookupService.ownerUsernameForCode("public")).thenReturn(null);
    when(analyticsQueryService.countClicks("public")).thenReturn(1L);

    assertEquals(1L, controller.count(null, "public"));
  }

  @Test
  void rejectsAnonymousAccessForOwnedLinks() {
    when(linkOwnershipLookupService.ownerUsernameForCode("demo")).thenReturn("owner");

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> controller.summary(null, "demo"));

    assertEquals(401, exception.getStatusCode().value());
  }

  @Test
  void rejectsDifferentUsersForOwnedLinks() {
    when(linkOwnershipLookupService.ownerUsernameForCode("demo")).thenReturn("owner");

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.summary(auth("someone-else", "USER"), "demo"));

    assertEquals(403, exception.getStatusCode().value());
  }

  @Test
  void allowsAdminAccessForOwnedLinks() {
    when(linkOwnershipLookupService.ownerUsernameForCode("demo")).thenReturn("owner");
    when(analyticsQueryService.countClicks("demo")).thenReturn(12L);

    assertEquals(12L, controller.count(auth("admin", "ADMIN"), "demo"));
  }

  private static Authentication auth(String username, String role) {
    return new UsernamePasswordAuthenticationToken(
        username, "n/a", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
  }
}
