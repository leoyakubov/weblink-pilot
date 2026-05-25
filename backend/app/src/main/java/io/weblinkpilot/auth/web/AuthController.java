package io.weblinkpilot.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.weblinkpilot.auth.service.AuthService;
import io.weblinkpilot.shared.contracts.AuthCredentialsRequest;
import io.weblinkpilot.shared.contracts.AuthResponse;
import io.weblinkpilot.shared.contracts.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register a user",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AuthCredentialsRequest.class))
            )
    )
    public AuthResponse register(@Valid @org.springframework.web.bind.annotation.RequestBody AuthCredentialsRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login and receive a JWT",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AuthCredentialsRequest.class))
            ),
            responses = { @ApiResponse(responseCode = "200", description = "JWT issued") }
    )
    public AuthResponse login(@Valid @org.springframework.web.bind.annotation.RequestBody AuthCredentialsRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Current signed-in user")
    @SecurityRequirement(name = "bearerAuth")
    public UserProfileResponse me(Authentication authentication) {
        return authService.profile(authentication);
    }
}
