package io.weblinkpilot.shared.contracts;

public record PasswordChangeRequest(String currentPassword, String newPassword) {}
