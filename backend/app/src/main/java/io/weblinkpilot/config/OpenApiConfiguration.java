package io.weblinkpilot.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@SecurityScheme(
    name = OpenApiConfiguration.BEARER_AUTH_SCHEME,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER)
public class OpenApiConfiguration {

  public static final String BEARER_AUTH_SCHEME = "bearerAuth";
}
