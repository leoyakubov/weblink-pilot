package io.weblinkpilot.links.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.IMAGE_PNG;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.weblinkpilot.links.exception.UrlNotFoundException;
import io.weblinkpilot.links.service.PublicUrlBuilder;
import io.weblinkpilot.links.service.QrCodeService;
import io.weblinkpilot.links.service.UrlLookupService;
import io.weblinkpilot.links.service.UrlService;
import io.weblinkpilot.shared.contracts.CreateLinkRequest;
import io.weblinkpilot.shared.contracts.LinkResponse;
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
class UrlControllerStandaloneTest {

  @Mock private UrlService urlService;

  @Mock private UrlLookupService urlLookupService;

  @Mock private QrCodeService qrCodeService;

  private PublicUrlBuilder publicUrlBuilder;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    publicUrlBuilder = new PublicUrlBuilder("http://localhost:8080");
    final UrlController controller =
        new UrlController(
            urlService,
            urlLookupService,
            qrCodeService,
            publicUrlBuilder,
            new SimpleMeterRegistry());
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new UrlExceptionHandler())
            .build();
  }

  @Test
  void createsLink() throws Exception {
    final LinkResponse response =
        new LinkResponse(
            "github-org",
            "http://localhost:8080/r/github-org",
            "http://localhost:8080/api/v1/urls/github-org/qr",
            "https://github.com/weblinkpilot/weblink-pilot/tree/main/docs",
            OffsetDateTime.now(ZoneOffset.UTC),
            null,
            0,
            null);
    when(urlService.create(any(CreateLinkRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/urls")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                                {
                                  "originalUrl": "https://github.com/weblinkpilot/weblink-pilot/tree/main/docs",
                                  "customAlias": "github-org"
                                }
                                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("github-org"))
        .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/r/github-org"))
        .andExpect(
            jsonPath("$.qrCodeUrl").value("http://localhost:8080/api/v1/urls/github-org/qr"));
  }

  @Test
  void listsRecentLinks() throws Exception {
    final LinkResponse first =
        new LinkResponse(
            "two",
            "http://localhost:8080/r/two",
            "http://localhost:8080/api/v1/urls/two/qr",
            "https://github.com/weblinkpilot/weblink-pilot/two",
            OffsetDateTime.now(ZoneOffset.UTC),
            null,
            0,
            null);
    final LinkResponse second =
        new LinkResponse(
            "one",
            "http://localhost:8080/r/one",
            "http://localhost:8080/api/v1/urls/one/qr",
            "https://github.com/weblinkpilot/weblink-pilot/one",
            OffsetDateTime.now(ZoneOffset.UTC),
            null,
            0,
            null);
    when(urlLookupService.listRecentLinks(10)).thenReturn(List.of(first, second));

    mockMvc
        .perform(get("/api/v1/urls"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("two"))
        .andExpect(jsonPath("$[1].code").value("one"));
  }

  @Test
  void returnsDetails() throws Exception {
    final LinkResponse response =
        new LinkResponse(
            "demo",
            "http://localhost:8080/r/demo",
            "http://localhost:8080/api/v1/urls/demo/qr",
            "https://github.com/weblinkpilot/weblink-pilot",
            OffsetDateTime.now(ZoneOffset.UTC),
            null,
            2,
            null);
    when(urlLookupService.getByCode("demo")).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/urls/demo"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("demo"))
        .andExpect(jsonPath("$.clickCount").value(2));
  }

  @Test
  void returnsPreview() throws Exception {
    final LinkResponse response =
        new LinkResponse(
            "demo",
            "http://localhost:8080/r/demo",
            "http://localhost:8080/api/v1/urls/demo/qr",
            "https://github.com/weblinkpilot/weblink-pilot",
            OffsetDateTime.now(ZoneOffset.UTC),
            null,
            2,
            null);
    when(urlLookupService.getByCode("demo")).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/urls/demo/preview"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(302))
        .andExpect(
            jsonPath("$.locationHeader").value("https://github.com/weblinkpilot/weblink-pilot"));
  }

  @Test
  void returnsQrImage() throws Exception {
    when(qrCodeService.generatePng("http://localhost:8080/q/demo"))
        .thenReturn(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47});

    mockMvc
        .perform(get("/api/v1/urls/demo/qr"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(IMAGE_PNG))
        .andExpect(content().bytes(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47}));
  }

  @Test
  void mapsNotFoundErrors() throws Exception {
    when(urlLookupService.getByCode("missing")).thenThrow(new UrlNotFoundException("missing"));

    mockMvc
        .perform(get("/api/v1/urls/missing"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.message", containsString("missing")));
  }
}
