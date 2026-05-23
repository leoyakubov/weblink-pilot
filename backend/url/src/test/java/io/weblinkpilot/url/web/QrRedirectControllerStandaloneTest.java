package io.weblinkpilot.url.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.weblinkpilot.shared.contracts.LinkTrackingSource;
import io.weblinkpilot.url.service.RedirectService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class QrRedirectControllerStandaloneTest {

    @Mock
    private RedirectService redirectService;

    @Mock
    private RequestContextExtractor requestContextExtractor;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        QrRedirectController controller = new QrRedirectController(redirectService, requestContextExtractor);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void redirectsAndTracksQrScansSeparately() throws Exception {
        RedirectRequestContext context = new RedirectRequestContext("127.0.0.1", "Mozilla/5.0", "https://github.com", "US");
        when(requestContextExtractor.extract(org.mockito.ArgumentMatchers.any(HttpServletRequest.class)))
                .thenReturn(context);
        when(redirectService.resolveTarget("demo", context, LinkTrackingSource.QR_SCAN))
                .thenReturn("https://example.com");

        mockMvc.perform(get("/q/demo"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com"));
    }
}
