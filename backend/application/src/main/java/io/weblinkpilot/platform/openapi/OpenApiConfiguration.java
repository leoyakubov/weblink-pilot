package io.weblinkpilot.platform.openapi;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@SecurityScheme(
    name = OpenApiConfiguration.BEARER_AUTH_SCHEME,
    type = SecuritySchemeType.HTTP,
    scheme = OpenApiConfiguration.BEARER_SCHEME,
    bearerFormat = OpenApiConfiguration.BEARER_FORMAT,
    in = SecuritySchemeIn.HEADER)
public class OpenApiConfiguration {

  public static final String BEARER_AUTH_SCHEME = "bearerAuth";
  public static final String BEARER_SCHEME = "bearer";
  public static final String BEARER_FORMAT = "JWT";
}
