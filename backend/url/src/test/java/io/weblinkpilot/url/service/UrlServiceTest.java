package io.weblinkpilot.url.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.shared.contracts.LinkResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlCreationService creationService;

    @Mock
    private UrlLookupService lookupService;

    @InjectMocks
    private UrlService service;

    @Test
    void delegatesCreateToCreationService() {
        LinkResponse response = new LinkResponse(
                "my-link",
                "http://localhost:8080/r/my-link",
                "http://localhost:8080/api/v1/urls/my-link/qr",
                "https://example.com",
                null,
                null,
                0
        );

        when(creationService.create(org.mockito.ArgumentMatchers.any())).thenReturn(response);

        LinkResponse result = service.create(new io.weblinkpilot.shared.contracts.CreateLinkRequest(
                "https://example.com",
                "my-link",
                null
        ));

        assertThat(result).isEqualTo(response);
        verify(creationService).create(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void delegatesReadToLookupService() {
        LinkResponse response = new LinkResponse(
                "abc123",
                "http://localhost:8080/r/abc123",
                "http://localhost:8080/api/v1/urls/abc123/qr",
                "https://example.com",
                null,
                null,
                3
        );

        when(lookupService.getByCode("abc123")).thenReturn(response);

        LinkResponse result = service.getByCode("abc123");

        assertThat(result).isEqualTo(response);
        verify(lookupService).getByCode("abc123");
    }
}
