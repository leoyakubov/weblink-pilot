package io.weblinkpilot.auth.config;

public final class AuthDefaults {

  public static final String ISSUER = "weblink-pilot";
  public static final long TOKEN_TTL_MINUTES = 240L;
  public static final long REFRESH_TOKEN_TTL_DAYS = 30L;

  private AuthDefaults() {}
}
