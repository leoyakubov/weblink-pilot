package io.weblinkpilot.platform.web;

import java.util.List;

public final class ApiRoutes {

  public static final String API_V1_PREFIX = "/api/v1/";
  public static final String ERROR = "/error";
  public static final String ALL_PATHS_PATTERN = "/**";
  public static final String REDIRECT_PREFIX = "/r/";
  public static final String QR_REDIRECT_PREFIX = "/q/";
  public static final String REDIRECT_PATTERN = "/r/**";
  public static final String QR_REDIRECT_PATTERN = "/q/**";
  public static final String ACTUATOR_HEALTH = "/actuator/health";
  public static final String ACTUATOR_INFO = "/actuator/info";
  public static final String ACTUATOR_METRICS = "/actuator/metrics";
  public static final String ACTUATOR_METRICS_PATTERN = "/actuator/metrics/**";
  public static final String ACTUATOR_PROMETHEUS = "/actuator/prometheus";
  public static final String SWAGGER_UI_PATTERN = "/swagger-ui/**";
  public static final String SWAGGER_UI_HTML = "/swagger-ui.html";
  public static final String OPENAPI_DOCS_PATTERN = "/v3/api-docs/**";
  public static final String AUTH_REGISTER = "/api/v1/auth/register";
  public static final String AUTH_LOGIN = "/api/v1/auth/login";
  public static final String AUTH_PASSWORD_RESET_REQUEST = "/api/v1/auth/password-reset/request";
  public static final String AUTH_PASSWORD_RESET_CONFIRM = "/api/v1/auth/password-reset/confirm";
  public static final String AUTH_EMAIL_VERIFICATION_REQUEST =
      "/api/v1/auth/email-verification/request";
  public static final String AUTH_EMAIL_VERIFICATION_CONFIRM =
      "/api/v1/auth/email-verification/confirm";
  public static final String AUTH_REFRESH = "/api/v1/auth/refresh";
  public static final String AUTH_LOGOUT = "/api/v1/auth/logout";
  public static final String AUTH_GITHUB_OAUTH_PATTERN = "/api/v1/auth/oauth2/github/**";
  public static final String AUTH_GITHUB_OAUTH_PREFIX = "/api/v1/auth/oauth2/github/";
  public static final String AUTH_ME = "/api/v1/auth/me";
  public static final String AUTH_ACCOUNT = "/api/v1/auth/account";
  public static final String AUTH_ACCOUNT_PASSWORD = "/api/v1/auth/account/password";
  public static final String ANALYTICS_PATTERN = "/api/v1/analytics/**";
  public static final String URLS_COLLECTION = "/api/v1/urls";
  public static final String URLS_ITEM_PATTERN = "/api/v1/urls/*";
  public static final String URLS_PREVIEW_PATTERN = "/api/v1/urls/*/preview";
  public static final String URLS_QR_PATTERN = "/api/v1/urls/*/qr";
  public static final String AI_LINK_METADATA_PATTERN = "/api/v1/ai/links/*/metadata";
  public static final String AI_LINK_METADATA_REGENERATE_PATTERN =
      "/api/v1/ai/links/*/metadata/regenerate";
  public static final String ADMIN_BASE = "/api/v1/admin";
  public static final String ADMIN_PATTERN = "/api/v1/admin/**";

  public static final List<String> PUBLIC_ACTUATOR_PATHS = List.of(ACTUATOR_HEALTH, ACTUATOR_INFO);
  public static final List<String> OBSERVABILITY_PATHS =
      List.of(ACTUATOR_METRICS, ACTUATOR_METRICS_PATTERN, ACTUATOR_PROMETHEUS);
  public static final List<String> API_DOCUMENTATION_PATHS =
      List.of(SWAGGER_UI_PATTERN, SWAGGER_UI_HTML, OPENAPI_DOCS_PATTERN);
  public static final List<String> PUBLIC_REDIRECT_PATHS =
      List.of(REDIRECT_PATTERN, QR_REDIRECT_PATTERN);

  public static final List<String> PUBLIC_AUTH_PATHS =
      List.of(
          AUTH_REGISTER,
          AUTH_LOGIN,
          AUTH_PASSWORD_RESET_REQUEST,
          AUTH_PASSWORD_RESET_CONFIRM,
          AUTH_EMAIL_VERIFICATION_REQUEST,
          AUTH_EMAIL_VERIFICATION_CONFIRM,
          AUTH_REFRESH,
          AUTH_LOGOUT);

  private ApiRoutes() {}
}
