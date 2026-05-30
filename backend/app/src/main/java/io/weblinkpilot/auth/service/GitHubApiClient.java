package io.weblinkpilot.auth.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class GitHubApiClient {

  private static final URI TOKEN_URI = URI.create("https://github.com/login/oauth/access_token");
  private static final URI API_URI = URI.create("https://api.github.com");
  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public GitHubApiClient(ObjectMapper objectMapper) {
    this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    this.objectMapper = objectMapper;
  }

  public String exchangeCodeForAccessToken(
      String clientId, String clientSecret, String code, String redirectUri) {
    String body =
        formBody(
            Map.of(
                "client_id", clientId,
                "client_secret", clientSecret,
                "code", code,
                "redirect_uri", redirectUri));
    HttpRequest request =
        HttpRequest.newBuilder(TOKEN_URI)
            .timeout(TIMEOUT)
            .header("Accept", "application/json")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    TokenResponse response = sendJson(request, TokenResponse.class);
    if (response.accessToken() == null || response.accessToken().isBlank()) {
      throw new IllegalStateException("GitHub did not return an access token");
    }
    return response.accessToken();
  }

  public GitHubProfile fetchProfile(String accessToken) {
    HttpRequest request =
        HttpRequest.newBuilder(API_URI.resolve("/user"))
            .timeout(TIMEOUT)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("Authorization", "Bearer " + accessToken)
            .header("User-Agent", "WebLinkPilot")
            .GET()
            .build();
    return sendJson(request, GitHubProfile.class);
  }

  public List<GitHubEmail> fetchEmails(String accessToken) {
    HttpRequest request =
        HttpRequest.newBuilder(API_URI.resolve("/user/emails"))
            .timeout(TIMEOUT)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("Authorization", "Bearer " + accessToken)
            .header("User-Agent", "WebLinkPilot")
            .GET()
            .build();
    GitHubEmail[] emails = sendJson(request, GitHubEmail[].class);
    return Arrays.asList(emails);
  }

  private <T> T sendJson(HttpRequest request, Class<T> responseType) {
    HttpResponse<String> response = send(request);
    if (response.statusCode() / 100 != 2) {
      throw new IllegalStateException(
          "GitHub request failed with status " + response.statusCode() + ": " + response.body());
    }
    try {
      return objectMapper.readValue(response.body(), responseType);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to parse GitHub response", exception);
    }
  }

  private HttpResponse<String> send(HttpRequest request) {
    try {
      return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("GitHub request was interrupted", exception);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to call GitHub", exception);
    }
  }

  private String formBody(Map<String, String> values) {
    return values.entrySet().stream()
        .map(
            entry ->
                encode(entry.getKey())
                    + "="
                    + encode(entry.getValue() == null ? "" : entry.getValue()))
        .collect(Collectors.joining("&"));
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record TokenResponse(@JsonProperty("access_token") String accessToken) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record GitHubProfile(long id, String login, String email) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record GitHubEmail(String email, boolean primary, boolean verified) {}
}
