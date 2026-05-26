package io.weblinkpilot.shared.contracts;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthCredentialsRequest(
    @NotBlank(message = "Username is required") String username,
    @NotBlank(message = "Password is required") String password,
    @Email(message = "Email must be valid") String email) {

  public AuthCredentialsRequest(String username, String password) {
    this(username, password, null);
  }
}
