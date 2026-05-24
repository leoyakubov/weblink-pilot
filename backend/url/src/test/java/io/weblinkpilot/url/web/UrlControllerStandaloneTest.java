package io.weblinkpilot.url.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.IMAGE_PNG;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.weblinkpilot.shared.contracts.CreateLinkRequest;
import io.weblinkpilot.shared.contracts.LinkResponse;
import io.weblinkpilot.shared.contracts.RedirectPreviewResponse;
import io.weblinkpilot.url.service.QrCodeService;
import io.weblinkpilot.url.service.PublicUrlBuilder;
import io.weblinkpilot.url.service.UrlLookupService;
import io.weblinkpilot.url.service.UrlService;
import io.weblinkpilot.url.exception.UrlNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.weblinkpilot.url.service.PublicUrlBuilder;

@ExtendWith(MockitoExtension.class)
class UrlControllerStandaloneTest {

    @Mock
    private UrlService urlService;

    @Mock
    private UrlLookupService urlLookupService;

    @Mock
    private QrCodeService qrCodeService;

    private PublicUrlBuilder publicUrlBuilder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        publicUrlBuilder = new PublicUrlBuilder("http://localhost:8080");
        UrlController controller = new UrlController(urlService, urlLookupService, qrCodeService, publicUrlBuilder, new SimpleMeterRegistry());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new UrlExceptionHandler())
                .build();
    }

    @Test
    void createsLink() throws Exception {
        LinkResponse response = new LinkResponse(
                "github-org",
                "http://localhost:8080/r/github-org",
                "http://localhost:8080/api/v1/urls/github-org/qr",
                "https://github.com/docs",
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                0,
                null
        );
        when(urlService.create(any(CreateLinkRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "https://github.com/docs",
                                  "customAlias": "github-org"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("github-org"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/r/github-org"))
                .andExpect(jsonPath("$.qrCodeUrl").value("http://localhost:8080/api/v1/urls/github-org/qr"));
    }

    @Test
    void listsRecentLinks() throws Exception {
        LinkResponse first = new LinkResponse(
                "two",
                "http://localhost:8080/r/two",
                "http://localhost:8080/api/v1/urls/two/qr",
                "https://example.com/two",
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                0,
                null
        );
        LinkResponse second = new LinkResponse(
                "one",
                "http://localhost:8080/r/one",
                "http://localhost:8080/api/v1/urls/one/qr",
                "https://example.com/one",
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                0,
                null
        );
        when(urlLookupService.listRecentLinks(10)).thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/v1/urls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("two"))
                .andExpect(jsonPath("$[1].code").value("one"));
    }

    @Test
    void returnsDetails() throws Exception {
        LinkResponse response = new LinkResponse(
                "demo",
                "http://localhost:8080/r/demo",
                "http://localhost:8080/api/v1/urls/demo/qr",
                "https://example.com",
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                2,
                null
        );
        when(urlLookupService.getByCode("demo")).thenReturn(response);

        mockMvc.perform(get("/api/v1/urls/demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("demo"))
                .andExpect(jsonPath("$.clickCount").value(2));
    }

    @Test
    void returnsPreview() throws Exception {
        LinkResponse response = new LinkResponse(
                "demo",
                "http://localhost:8080/r/demo",
                "http://localhost:8080/api/v1/urls/demo/qr",
                "https://example.com",
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                2,
                null
        );
        when(urlLookupService.getByCode("demo")).thenReturn(response);

        mockMvc.perform(get("/api/v1/urls/demo/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(302))
                .andExpect(jsonPath("$.locationHeader").value("https://example.com"));
    }

    @Test
    void returnsQrImage() throws Exception {
        when(qrCodeService.generatePng("http://localhost:8080/q/demo")).thenReturn(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47});

        mockMvc.perform(get("/api/v1/urls/demo/qr"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(IMAGE_PNG))
                .andExpect(content().bytes(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47}));
    }

    @Test
    void mapsNotFoundErrors() throws Exception {
        when(urlLookupService.getByCode("missing")).thenThrow(new UrlNotFoundException("missing"));

        mockMvc.perform(get("/api/v1/urls/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message", containsString("missing")));
    }
}
