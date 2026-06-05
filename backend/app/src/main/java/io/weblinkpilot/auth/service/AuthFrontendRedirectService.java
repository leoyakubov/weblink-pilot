package io.weblinkpilot.auth.service;

import io.weblinkpilot.auth.config.AuthProperties;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

@Service
public class AuthFrontendRedirectService {

  private final String frontendBaseUrl;

  public AuthFrontendRedirectService(AuthProperties authProperties) {
    this.frontendBaseUrl = authProperties.getFrontendBaseUrl();
  }

  public URI buildGithubCompleteUri(String ticket) {
    return URI.create(
        frontendBaseUrl
            + "/auth/github/complete#ticket="
            + URLEncoder.encode(ticket, StandardCharsets.UTF_8));
  }
}
