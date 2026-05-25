package io.weblinkpilot.url.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class RedirectControllerStandaloneTest {

  @Mock private RedirectService redirectService;

  @Mock private RequestContextExtractor requestContextExtractor;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    RedirectController controller =
        new RedirectController(redirectService, requestContextExtractor);
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  void redirectsToOriginalUrl() throws Exception {
    RedirectRequestContext context =
        new RedirectRequestContext("127.0.0.1", "Mozilla/5.0", "https://github.com", "US");
    when(requestContextExtractor.extract(
            org.mockito.ArgumentMatchers.any(HttpServletRequest.class)))
        .thenReturn(context);
    when(redirectService.resolveTarget(
            "demo",
            new RedirectRequestContext("127.0.0.1", "Mozilla/5.0", "https://github.com", "US")))
        .thenReturn("https://github.com/weblinkpilot/weblink-pilot");

    mockMvc
        .perform(get("/r/demo"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "https://github.com/weblinkpilot/weblink-pilot"));
  }
}
