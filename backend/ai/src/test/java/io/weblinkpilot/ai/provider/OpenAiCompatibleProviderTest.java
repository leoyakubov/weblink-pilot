package io.weblinkpilot.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.weblinkpilot.ai.config.AiProperties;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleProviderTest {

  private HttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void generatesMetadataFromChatCompletionJsonResponse() throws IOException {
    AtomicReference<String> authorization = new AtomicReference<>();
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/chat/completions",
        exchange -> {
          authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
          byte[] body =
              """
              {
                "choices": [
                  {
                    "message": {
                      "content": "{\\"title\\":\\"Vue Guide\\",\\"summary\\":\\"Official Vue introduction.\\",\\"category\\":\\"Frontend\\",\\"tags\\":[\\"vue\\",\\"javascript\\"],\\"icon\\":\\"code\\",\\"suggestedAlias\\":\\"vue-guide\\"}"
                    }
                  }
                ]
              }
              """
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();

    AiProperties properties = new AiProperties();
    properties.setProvider("openai");
    properties.getOpenai().setBaseUrl("http://localhost:" + server.getAddress().getPort());
    properties.getOpenai().setApiKey("test-key");
    properties.getOpenai().setModel("demo-model");
    ObjectMapper objectMapper = new ObjectMapper();
    OpenAiCompatibleProvider provider =
        new OpenAiCompatibleProvider(
            properties,
            objectMapper,
            new AiMetadataPromptRenderer(),
            new AiMetadataJsonParser(objectMapper));

    var result =
        provider.generateLinkMetadata(
            new AiLinkMetadataPrompt("vue-js", "https://vuejs.org/guide/introduction.html", null));

    assertThat(authorization.get()).isEqualTo("Bearer test-key");
    assertThat(result.title()).isEqualTo("Vue Guide");
    assertThat(result.category()).isEqualTo("Frontend");
    assertThat(result.tags()).containsExactly("vue", "javascript");
    assertThat(result.suggestedAlias()).isEqualTo("vue-guide");
  }
}
