package io.weblinkpilot.url.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UrlApiIntegrationTest {

    private static final String AUTH_USER = "admin";
    private static final String AUTH_PASSWORD = "admin123";

    @Autowired
    private WebApplicationContext webApplicationContext;

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
                                  "originalUrl": "https://example.com",
                                  "customAlias": "demo-it"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("demo-it"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/r/demo-it"))
                .andExpect(jsonPath("$.qrCodeUrl").value("http://localhost:8080/api/v1/urls/demo-it/qr"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com"));

        mockMvc.perform(get("/r/demo-it"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com"));

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
    void returnsConflictForDuplicateAlias() throws Exception {
        String payload = """
                {
                  "originalUrl": "https://example.com",
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
    void exposesOpenApiDocsPublicly() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }
}
