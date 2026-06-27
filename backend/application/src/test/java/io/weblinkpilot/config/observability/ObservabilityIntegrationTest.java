package io.weblinkpilot.config.observability;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.weblinkpilot.auth.config.BootstrapDefaults;
import io.weblinkpilot.auth.config.RoleNames;
import io.weblinkpilot.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ObservabilityIntegrationTest {

  private static final String AUTH_USER = BootstrapDefaults.ADMIN_USERNAME;

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
  }

  @Test
  void exposesPrometheusMetricsForBusinessEvents() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "originalUrl": "https://github.com/weblinkpilot/weblink-pilot",
                                  "customAlias": "metrics-demo"
                                }
                                """))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/v1/urls/metrics-demo/qr")).andExpect(status().isOk());

    mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isForbidden());

    String token = jwtService.issueToken(AUTH_USER, RoleNames.ADMIN);

    mockMvc
        .perform(get("/actuator/prometheus").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("weblinkpilot_links_created_events_total 1.0")))
        .andExpect(content().string(containsString("weblinkpilot_urls_qr_requests_total 1.0")));
  }
}
