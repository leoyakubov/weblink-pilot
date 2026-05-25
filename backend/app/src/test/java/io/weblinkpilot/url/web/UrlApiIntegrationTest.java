package io.weblinkpilot.url.web;

import io.weblinkpilot.auth.config.BootstrapDefaults;
import io.weblinkpilot.url.domain.ShortLink;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import io.weblinkpilot.url.service.UrlCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.auth.jwt-secret=test-jwt-secret-for-tests-only"
})
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UrlApiIntegrationTest {

    private static final String AUTH_USER = BootstrapDefaults.ADMIN_USERNAME;
    private static final String AUTH_PASSWORD = BootstrapDefaults.ADMIN_PASSWORD;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ShortLinkRepository shortLinkRepository;

    @Autowired
    private UrlCacheService urlCacheService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void createsLinkAndRedirectsPublicly() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .with(httpBasic(AUTH_USER, AUTH_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "https://github.com/weblinkpilot/weblink-pilot",
                                  "customAlias": "demo-it"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("demo-it"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/r/demo-it"))
                .andExpect(jsonPath("$.qrCodeUrl").value("http://localhost:8080/api/v1/urls/demo-it/qr"))
                .andExpect(jsonPath("$.originalUrl").value("https://github.com/weblinkpilot/weblink-pilot"));

        mockMvc.perform(get("/r/demo-it")
                        .header("Accept-Language", "en-US,en;q=0.9"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://github.com/weblinkpilot/weblink-pilot"));

        mockMvc.perform(get("/api/v1/urls/demo-it/preview")
                        .with(httpBasic(AUTH_USER, AUTH_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("demo-it"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/r/demo-it"))
                .andExpect(jsonPath("$.targetUrl").value("https://github.com/weblinkpilot/weblink-pilot"))
                .andExpect(jsonPath("$.status").value(302))
                .andExpect(jsonPath("$.locationHeader").value("https://github.com/weblinkpilot/weblink-pilot"));

        mockMvc.perform(get("/api/v1/analytics/demo-it")
                        .with(httpBasic(AUTH_USER, AUTH_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topCountries[0].country").value("US"))
                .andExpect(jsonPath("$.topCountries[0].clicks").value(1));

        MvcResult qrResult = mockMvc.perform(get("/api/v1/urls/demo-it/qr"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn();

        byte[] png = qrResult.getResponse().getContentAsByteArray();
        assertThat(png).isNotEmpty();
        assertThat(png[0]).isEqualTo((byte) 0x89);
        assertThat(png[1]).isEqualTo((byte) 0x50);
        assertThat(png[2]).isEqualTo((byte) 0x4E);
        assertThat(png[3]).isEqualTo((byte) 0x47);
    }

    @Test
    void listsRecentLinksForAuthenticatedUsers() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .with(httpBasic(AUTH_USER, AUTH_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "https://github.com/weblinkpilot/weblink-pilot/one",
                                  "customAlias": "one"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/urls")
                        .with(httpBasic(AUTH_USER, AUTH_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "https://github.com/weblinkpilot/weblink-pilot/two",
                                  "customAlias": "two"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/urls")
                        .with(httpBasic(AUTH_USER, AUTH_PASSWORD))
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("two"))
                .andExpect(jsonPath("$[0].shortUrl").value("http://localhost:8080/r/two"))
                .andExpect(jsonPath("$[1].code").value("one"))
                .andExpect(jsonPath("$[1].shortUrl").value("http://localhost:8080/r/one"));
    }

    @Test
    void returnsConflictForDuplicateAlias() throws Exception {
        String payload = """
                {
                  "originalUrl": "https://github.com/weblinkpilot/weblink-pilot",
                  "customAlias": "demo-duplicate"
                }
                """;

        mockMvc.perform(post("/api/v1/urls")
                        .with(httpBasic(AUTH_USER, AUTH_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/urls")
                        .with(httpBasic(AUTH_USER, AUTH_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message", containsString("demo-duplicate")));
    }

    @Test
    void rejectsInvalidCreatePayloads() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .with(httpBasic(AUTH_USER, AUTH_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "   ",
                                  "customAlias": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("Original URL is required")));
    }

    @Test
    void createsRandomCodeWhenAliasIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .with(httpBasic(AUTH_USER, AUTH_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "https://github.com/weblinkpilot/weblink-pilot/random-code"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern("^[A-Za-z0-9]{7}$")))
                .andExpect(jsonPath("$.shortUrl").value(org.hamcrest.Matchers.matchesPattern("http://localhost:8080/r/[A-Za-z0-9]{7}")))
                .andExpect(jsonPath("$.qrCodeUrl").value(org.hamcrest.Matchers.matchesPattern("http://localhost:8080/api/v1/urls/[A-Za-z0-9]{7}/qr")))
                .andExpect(jsonPath("$.originalUrl").value("https://github.com/weblinkpilot/weblink-pilot/random-code"));
    }
    @Test
    void returnsGoneForExpiredRedirects() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .with(httpBasic(AUTH_USER, AUTH_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "https://github.com/weblinkpilot/weblink-pilot",
                                  "customAlias": "expired-it"
                                }
                                """))
                .andExpect(status().isOk());

        ShortLink link = shortLinkRepository.findByCode("expired-it").orElseThrow();
        setExpiresAt(link, OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5));
        shortLinkRepository.saveAndFlush(link);
        urlCacheService.evict("expired-it");

        mockMvc.perform(get("/r/expired-it"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("LINK_EXPIRED"))
                .andExpect(jsonPath("$.message", containsString("expired-it")));
    }

    private static void setExpiresAt(ShortLink link, OffsetDateTime expiresAt) {
        try {
            Field field = ShortLink.class.getDeclaredField("expiresAt");
            field.setAccessible(true);
            field.set(link, expiresAt);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to update expiresAt in test", exception);
        }
    }

    @Test
    void exposesOpenApiDocsPublicly() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }
}
