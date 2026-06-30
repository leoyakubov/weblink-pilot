package io.weblinkpilot.auth.integration.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.weblinkpilot.auth.config.AuthProperties;
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
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "ObjectMapper is a shared Spring bean and is safe to retain.")
public class GitHubApiClient {

  private static final String ACCEPT_HEADER = "Accept";
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String GITHUB_API_VERSION_HEADER = "X-GitHub-Api-Version";
  private static final String USER_AGENT_HEADER = "User-Agent";
  private static final String JSON_MEDIA_TYPE = "application/json";
  private static final String FORM_MEDIA_TYPE = "application/x-www-form-urlencoded";
  private static final String GITHUB_JSON_MEDIA_TYPE = "application/vnd.github+json";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String USER_PATH = "/user";
  private static final String USER_EMAILS_PATH = "/user/emails";

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final URI tokenUri;
  private final URI apiBaseUri;
  private final String apiVersion;
  private final String userAgent;
  private final Duration timeout;

  public GitHubApiClient(ObjectMapper objectMapper, AuthProperties authProperties) {
    AuthProperties.GitHub github = authProperties.getGithub();
    this.tokenUri = github.getTokenUrl();
    this.apiBaseUri = github.getApiBaseUrl();
    this.apiVersion = github.getApiVersion();
    this.userAgent = github.getUserAgent();
    this.timeout = github.getTimeout();
    this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
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
        HttpRequest.newBuilder(tokenUri)
            .timeout(timeout)
            .header(ACCEPT_HEADER, JSON_MEDIA_TYPE)
            .header(CONTENT_TYPE_HEADER, FORM_MEDIA_TYPE)
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
        HttpRequest.newBuilder(apiBaseUri.resolve(USER_PATH))
            .timeout(timeout)
            .header(ACCEPT_HEADER, GITHUB_JSON_MEDIA_TYPE)
            .header(GITHUB_API_VERSION_HEADER, apiVersion)
            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken)
            .header(USER_AGENT_HEADER, userAgent)
            .GET()
            .build();
    return sendJson(request, GitHubProfile.class);
  }

  public List<GitHubEmail> fetchEmails(String accessToken) {
    HttpRequest request =
        HttpRequest.newBuilder(apiBaseUri.resolve(USER_EMAILS_PATH))
            .timeout(timeout)
            .header(ACCEPT_HEADER, GITHUB_JSON_MEDIA_TYPE)
            .header(GITHUB_API_VERSION_HEADER, apiVersion)
            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken)
            .header(USER_AGENT_HEADER, userAgent)
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
