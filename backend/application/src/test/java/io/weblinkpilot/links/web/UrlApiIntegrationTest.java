package io.weblinkpilot.links.web;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.weblinkpilot.links.exception.UrlNotFoundException;
import io.weblinkpilot.links.service.PublicUrlBuilder;
import io.weblinkpilot.links.service.QrCodeService;
import io.weblinkpilot.links.service.UrlLookupService;
import io.weblinkpilot.links.service.UrlService;
import io.weblinkpilot.shared.contracts.CreateLinkRequest;
import io.weblinkpilot.shared.contracts.LinkResponse;
import io.weblinkpilot.shared.contracts.RedirectPreviewResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class UrlApiIntegrationTest {

  @Mock private UrlService urlService;

  @Mock private UrlLookupService urlLookupService;

  @Mock private QrCodeService qrCodeService;

  @Mock private PublicUrlBuilder publicUrlBuilder;

  private UrlController controller;

  @BeforeEach
  void setUp() {
    controller =
        new UrlController(
            urlService,
            urlLookupService,
            qrCodeService,
            publicUrlBuilder,
            new SimpleMeterRegistry());
  }

  @Test
  void createsLinkAndRedirectsPublicly() {
    LinkResponse response =
        new LinkResponse(
            "demo-it",
            "http://localhost:8080/r/demo-it",
            "http://localhost:8080/api/v1/urls/demo-it/qr",
            "https://github.com/weblinkpilot/weblink-pilot",
            OffsetDateTime.now(ZoneOffset.UTC),
            null,
            0,
            null);
    when(urlService.create(any(CreateLinkRequest.class), any())).thenReturn(response);

    LinkResponse actual =
        controller
            .create(
                auth("alice", "USER"),
                new CreateLinkRequest(
                    "https://github.com/weblinkpilot/weblink-pilot", "demo-it", null))
            .getBody();

    assertEquals("demo-it", actual.code());
    assertEquals("http://localhost:8080/r/demo-it", actual.shortUrl());
    assertEquals("http://localhost:8080/api/v1/urls/demo-it/qr", actual.qrCodeUrl());
  }

  @Test
  void listsRecentLinksForAuthenticatedUsers() {
    LinkResponse first =
        new LinkResponse(
            "two",
            "http://localhost:8080/r/two",
            "http://localhost:8080/api/v1/urls/two/qr",
            "https://github.com/weblinkpilot/weblink-pilot/two",
            OffsetDateTime.now(ZoneOffset.UTC),
            null,
            0,
            null);
    LinkResponse second =
        new LinkResponse(
            "one",
            "http://localhost:8080/r/one",
            "http://localhost:8080/api/v1/urls/one/qr",
            "https://github.com/weblinkpilot/weblink-pilot/one",
            OffsetDateTime.now(ZoneOffset.UTC),
            null,
            0,
            null);
    when(urlLookupService.listRecentLinks("alice", false, null, 10))
        .thenReturn(List.of(first, second));

    List<LinkResponse> response = controller.list(auth("alice", "USER"), 10, null).getBody();

    assertEquals("two", response.get(0).code());
    assertEquals("one", response.get(1).code());
  }

  @Test
  void returnsDetails() {
    LinkResponse response =
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

    LinkResponse actual = controller.details("demo").getBody();

    assertEquals("demo", actual.code());
    assertEquals(2L, actual.clickCount());
  }

  @Test
  void returnsPreview() {
    LinkResponse response =
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

    RedirectPreviewResponse actual = controller.preview("demo").getBody();

    assertEquals(302, actual.status());
    assertEquals("https://github.com/weblinkpilot/weblink-pilot", actual.locationHeader());
  }

  @Test
  void returnsQrImage() {
    when(publicUrlBuilder.buildQrScanUrl("demo")).thenReturn("http://localhost:8080/q/demo");
    when(qrCodeService.generatePng("http://localhost:8080/q/demo"))
        .thenReturn(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47});

    var actual = controller.qr("demo");

    assertEquals(MediaType.IMAGE_PNG, actual.getHeaders().getContentType());
    assertArrayEquals(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47}, actual.getBody());
  }

  @Test
  void returnsRandomCodeWhenAliasIsBlank() {
    LinkResponse response =
        new LinkResponse(
            "abc1234",
            "http://localhost:8080/r/abc1234",
            "http://localhost:8080/api/v1/urls/abc1234/qr",
            "https://github.com/weblinkpilot/weblink-pilot/random-code",
            OffsetDateTime.now(ZoneOffset.UTC),
            null,
            0,
            null);
    when(urlService.create(any(CreateLinkRequest.class), any())).thenReturn(response);

    LinkResponse actual =
        controller
            .create(
                auth("alice", "USER"),
                new CreateLinkRequest(
                    "https://github.com/weblinkpilot/weblink-pilot/random-code", null, null))
            .getBody();

    assertEquals("abc1234", actual.code());
    assertEquals("http://localhost:8080/r/abc1234", actual.shortUrl());
    assertEquals("http://localhost:8080/api/v1/urls/abc1234/qr", actual.qrCodeUrl());
  }

  @Test
  void rejectsMissingLinks() {
    when(urlLookupService.getByCode("missing")).thenThrow(new UrlNotFoundException("missing"));

    assertThrows(UrlNotFoundException.class, () -> controller.details("missing"));
  }

  private static Authentication auth(String username, String role) {
    return new UsernamePasswordAuthenticationToken(
        username, "n/a", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
  }
}
