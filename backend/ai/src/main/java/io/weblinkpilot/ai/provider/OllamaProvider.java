package io.weblinkpilot.ai.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.weblinkpilot.ai.config.AiProperties;
import io.weblinkpilot.ai.domain.AiLinkMetadataResult;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification =
        "Spring-managed configuration and ObjectMapper beans are intentionally retained.")
public class OllamaProvider implements AiProvider {

  private final AiProperties properties;
  private final ObjectMapper objectMapper;
  private final AiMetadataPromptRenderer promptRenderer;
  private final AiMetadataJsonParser jsonParser;
  private final HttpClient httpClient;

  public OllamaProvider(
      AiProperties properties,
      ObjectMapper objectMapper,
      AiMetadataPromptRenderer promptRenderer,
      AiMetadataJsonParser jsonParser) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.promptRenderer = promptRenderer;
    this.jsonParser = jsonParser;
    this.httpClient = HttpClient.newHttpClient();
  }

  @Override
  public String name() {
    return "ollama";
  }

  @Override
  public AiLinkMetadataResult generateLinkMetadata(AiLinkMetadataPrompt prompt) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder(endpoint())
              .timeout(timeout())
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(requestBody(prompt)))
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException("Ollama returned HTTP " + response.statusCode());
      }
      return parseResponse(response.body());
    } catch (IOException exception) {
      throw new IllegalStateException("Could not call Ollama", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Ollama request interrupted", exception);
    }
  }

  private URI endpoint() {
    String baseUrl = properties.getOllama().getBaseUrl().replaceAll("/+$", "");
    return URI.create(baseUrl + "/api/generate");
  }

  private Duration timeout() {
    Duration timeout = properties.getOllama().getTimeout();
    return timeout == null || timeout.isNegative() || timeout.isZero()
        ? Duration.ofSeconds(10)
        : timeout;
  }

  private String requestBody(AiLinkMetadataPrompt prompt) throws JsonProcessingException {
    return objectMapper.writeValueAsString(
        new OllamaGenerateRequest(
            properties.getOllama().getModel(),
            promptRenderer.generationPrompt(prompt),
            false,
            "json",
            new OllamaOptions(0.2)));
  }

  private AiLinkMetadataResult parseResponse(String body) throws JsonProcessingException {
    JsonNode root = objectMapper.readTree(body);
    JsonNode response = root.get("response");
    if (response == null || response.asText().isBlank()) {
      throw new IllegalStateException("Ollama response did not include generated metadata");
    }

    return jsonParser.parse(response.asText());
  }

  private record OllamaGenerateRequest(
      String model, String prompt, boolean stream, String format, OllamaOptions options) {}

  private record OllamaOptions(double temperature) {}
}
