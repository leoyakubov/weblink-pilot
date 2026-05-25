package io.weblinkpilot.config.rate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.weblinkpilot.auth.config.BootstrapDefaults;
import io.weblinkpilot.config.RequestIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(
    properties = {
      "app.rate-limit.enabled=true",
      "app.rate-limit.api-per-minute=1",
      "app.rate-limit.redirect-per-minute=1"
    })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RateLimitIntegrationTest {

  private static final String AUTH_USER = BootstrapDefaults.ADMIN_USERNAME;
  private static final String AUTH_PASSWORD = BootstrapDefaults.ADMIN_PASSWORD;

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired private RequestIdFilter requestIdFilter;

  @Autowired private RateLimitFilter rateLimitFilter;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .addFilters(requestIdFilter, rateLimitFilter)
            .build();
  }

  @Test
  void limitsApiWritesPerIp() throws Exception {
    String payload =
        """
                {
                  "originalUrl": "https://github.com/weblinkpilot/weblink-pilot",
                  "customAlias": "rl-api"
                }
                """;

    mockMvc
        .perform(
            post("/api/v1/urls")
                .with(httpBasic(AUTH_USER, AUTH_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(header().string("X-RateLimit-Limit", "1"))
        .andExpect(header().string("X-RateLimit-Remaining", "0"));

    mockMvc
        .perform(
            post("/api/v1/urls")
                .with(httpBasic(AUTH_USER, AUTH_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "originalUrl": "https://example.org",
                                  "customAlias": "rl-api-2"
                                }
                                """))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
        .andExpect(jsonPath("$.status").value(429))
        .andExpect(header().exists("Retry-After"));
  }

  @Test
  void limitsRedirectsPerIp() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/urls")
                .with(httpBasic(AUTH_USER, AUTH_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "originalUrl": "https://github.com/weblinkpilot/weblink-pilot",
                                  "customAlias": "rl-redirect"
                                }
                                """))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/r/rl-redirect"))
        .andExpect(status().isFound())
        .andExpect(header().string("X-RateLimit-Limit", "1"))
        .andExpect(header().string("X-RateLimit-Remaining", "0"));

    mockMvc
        .perform(get("/r/rl-redirect"))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
        .andExpect(jsonPath("$.status").value(429))
        .andExpect(header().exists("Retry-After"));
  }
}
