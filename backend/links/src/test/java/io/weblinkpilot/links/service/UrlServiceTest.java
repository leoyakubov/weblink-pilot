package io.weblinkpilot.links.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.shared.api.links.LinkResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

  @Mock private UrlCreationService creationService;

  @Mock private UrlLookupService lookupService;

  @InjectMocks private UrlService service;

  @Test
  void delegatesCreateToCreationService() {
    LinkResponse response =
        new LinkResponse(
            "my-link",
            "http://localhost:8080/r/my-link",
            "http://localhost:8080/api/v1/urls/my-link/qr",
            "https://github.com/weblinkpilot/weblink-pilot",
            null,
            null,
            0,
            null);

    when(creationService.create(org.mockito.ArgumentMatchers.any())).thenReturn(response);

    LinkResponse result =
        service.create(
            new io.weblinkpilot.shared.api.links.CreateLinkRequest(
                "https://github.com/weblinkpilot/weblink-pilot", "my-link", null));

    assertThat(result).isEqualTo(response);
    verify(creationService).create(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void delegatesReadToLookupService() {
    LinkResponse response =
        new LinkResponse(
            "abc123",
            "http://localhost:8080/r/abc123",
            "http://localhost:8080/api/v1/urls/abc123/qr",
            "https://github.com/weblinkpilot/weblink-pilot",
            null,
            null,
            3,
            null);

    when(lookupService.getByCode("abc123")).thenReturn(response);

    LinkResponse result = service.getByCode("abc123");

    assertThat(result).isEqualTo(response);
    verify(lookupService).getByCode("abc123");
  }
}
