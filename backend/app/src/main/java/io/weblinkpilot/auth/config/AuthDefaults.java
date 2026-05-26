package io.weblinkpilot.auth.config;

public final class AuthDefaults {

  public static final String ISSUER = "weblink-pilot";
  public static final long TOKEN_TTL_MINUTES = 30L;
  public static final long REFRESH_TOKEN_TTL_DAYS = 30L;
  public static final String REFRESH_COOKIE_NAME = "weblinkpilot_refresh";
  public static final String REFRESH_COOKIE_PATH = "/api/v1/auth";
  public static final String REFRESH_COOKIE_SAME_SITE = "Lax";
  public static final boolean REFRESH_COOKIE_SECURE = false;

  private AuthDefaults() {}
}
