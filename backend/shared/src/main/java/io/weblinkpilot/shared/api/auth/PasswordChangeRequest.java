package io.weblinkpilot.shared.api.auth;

public record PasswordChangeRequest(String currentPassword, String newPassword) {}
