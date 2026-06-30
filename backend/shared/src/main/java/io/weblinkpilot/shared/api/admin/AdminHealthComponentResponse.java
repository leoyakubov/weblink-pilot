package io.weblinkpilot.shared.api.admin;

public record AdminHealthComponentResponse(
    String name, String status, String detail, AdminHealthErrorResponse error) {

  public AdminHealthComponentResponse(String name, String status, String detail) {
    this(name, status, detail, null);
  }
}
