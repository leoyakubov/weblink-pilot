package io.weblinkpilot.ai.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.util.List;
import org.springframework.stereotype.Component;

@Component
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification =
        "Spring-managed configuration and ObjectMapper beans are intentionally retained.")
public class OpenAiCompatibleProvider implements AiProvider {

  private final AiProperties properties;
  private final ObjectMapper objectMapper;
  private final AiMetadataPromptRenderer promptRenderer;
  private final AiMetadataJsonParser jsonParser;
  private final HttpClient httpClient;

  public OpenAiCompatibleProvider(
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
    return "openai";
  }

  @Override
  public AiLinkMetadataResult generateLinkMetadata(AiLinkMetadataPrompt prompt) {
    if (properties.getOpenai().getApiKey() == null
        || properties.getOpenai().getApiKey().isBlank()) {
      throw new IllegalStateException("OpenAI-compatible provider requires an API key");
    }

    try {
      HttpRequest request =
          HttpRequest.newBuilder(endpoint())
              .timeout(timeout())
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + properties.getOpenai().getApiKey())
              .POST(HttpRequest.BodyPublishers.ofString(requestBody(prompt)))
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException(
            "OpenAI-compatible provider returned HTTP " + response.statusCode());
      }
      return parseResponse(response.body());
    } catch (IOException exception) {
      throw new IllegalStateException("Could not call OpenAI-compatible provider", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("OpenAI-compatible request interrupted", exception);
    }
  }

  private URI endpoint() {
    String baseUrl = properties.getOpenai().getBaseUrl().replaceAll("/+$", "");
    return URI.create(baseUrl + "/chat/completions");
  }

  private Duration timeout() {
    Duration timeout = properties.getOpenai().getTimeout();
    return timeout == null || timeout.isNegative() || timeout.isZero()
        ? Duration.ofSeconds(10)
        : timeout;
  }

  private String requestBody(AiLinkMetadataPrompt prompt) throws JsonProcessingException {
    return objectMapper.writeValueAsString(
        new ChatCompletionRequest(
            properties.getOpenai().getModel(),
            List.of(
                new Message("system", promptRenderer.systemPrompt()),
                new Message("user", promptRenderer.userPrompt(prompt))),
            new ResponseFormat("json_object"),
            0.2));
  }

  private AiLinkMetadataResult parseResponse(String body) throws JsonProcessingException {
    JsonNode root = objectMapper.readTree(body);
    JsonNode content = root.at("/choices/0/message/content");
    if (content.isMissingNode() || content.asText().isBlank()) {
      throw new IllegalStateException("OpenAI-compatible response did not include metadata");
    }

    return jsonParser.parse(content.asText());
  }

  private record ChatCompletionRequest(
      String model,
      List<Message> messages,
      @JsonProperty("response_format") ResponseFormat responseFormat,
      double temperature) {}

  private record Message(String role, String content) {}

  private record ResponseFormat(String type) {}
}
