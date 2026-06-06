package io.weblinkpilot.auth.service;

public interface AccountNotificationService {

  void sendPasswordResetLink(String email, String link);

  void sendEmailVerificationLink(String email, String link);
}
