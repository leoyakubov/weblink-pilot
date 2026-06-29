package io.weblinkpilot.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.weblinkpilot.ai.config.AiProperties;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OllamaProviderTest {

  private HttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void generatesMetadataFromOllamaJsonResponse() throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/api/generate",
        exchange -> {
          byte[] body =
              """
              {
                "response": "{\\"title\\":\\"Spring Boot\\",\\"summary\\":\\"Spring Boot project docs.\\",\\"category\\":\\"Programming\\",\\"tags\\":[\\"spring\\",\\"java\\"],\\"icon\\":\\"code\\",\\"suggestedAlias\\":\\"spring-boot\\"}"
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
    properties.setProvider("ollama");
    properties.getOllama().setBaseUrl("http://localhost:" + server.getAddress().getPort());
    properties.getOllama().setModel("llama3.2:1b");
    OllamaProvider provider = new OllamaProvider(properties, new ObjectMapper());

    var result =
        provider.generateLinkMetadata(
            new AiLinkMetadataPrompt(
                "spring-boot", "https://spring.io/projects/spring-boot", null));

    assertThat(result.title()).isEqualTo("Spring Boot");
    assertThat(result.category()).isEqualTo("Programming");
    assertThat(result.tags()).containsExactly("spring", "java");
    assertThat(result.suggestedAlias()).isEqualTo("spring-boot");
  }
}
