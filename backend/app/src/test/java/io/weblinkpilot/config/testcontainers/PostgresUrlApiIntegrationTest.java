package io.weblinkpilot.config.testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.weblinkpilot.url.repository.ShortLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import io.weblinkpilot.auth.config.BootstrapDefaults;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class PostgresUrlApiIntegrationTest {

    private static final String AUTH_USER = BootstrapDefaults.ADMIN_USERNAME;
    private static final String AUTH_PASSWORD = BootstrapDefaults.ADMIN_PASSWORD;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("weblinkpilot")
            .withUsername("weblink")
            .withPassword("weblink");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ShortLinkRepository shortLinkRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void createsAndStoresLinksInPostgres() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .with(httpBasic(AUTH_USER, AUTH_PASSWORD))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "https://github.com",
                                  "customAlias": "pg-demo"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("pg-demo"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/r/pg-demo"));

        org.assertj.core.api.Assertions.assertThat(shortLinkRepository.findByCode("pg-demo")).isPresent();
    }
}
