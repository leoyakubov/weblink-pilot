package io.weblinkpilot.auth.config;

public final class AuthDefaults {

  public static final String ISSUER = "weblink-pilot";
  public static final long TOKEN_TTL_MINUTES = 30L;
  public static final long REFRESH_TOKEN_TTL_DAYS = 30L;
  public static final long ACCOUNT_ACTION_TOKEN_TTL_HOURS = 24L;
  public static final long GITHUB_LOGIN_TICKET_TTL_MINUTES = 10L;
  public static final String REFRESH_COOKIE_NAME = "weblinkpilot_refresh";
  public static final String REFRESH_COOKIE_PATH = "/api/v1/auth";
  public static final String REFRESH_COOKIE_SAME_SITE = "Lax";
  public static final boolean REFRESH_COOKIE_SECURE = false;
  public static final String FRONTEND_BASE_URL = "http://localhost:5173";
  public static final String GITHUB_CLIENT_ID = "";
  public static final String GITHUB_CLIENT_SECRET = "";
  public static final String GITHUB_SCOPE = "read:user user:email";
  public static final String GITHUB_STATE_COOKIE_NAME = "weblinkpilot_github_oauth_state";
  public static final String GITHUB_STATE_COOKIE_PATH = "/api/v1/auth/oauth2/github";

  private AuthDefaults() {}
}
