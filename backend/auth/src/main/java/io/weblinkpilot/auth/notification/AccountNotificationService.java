package io.weblinkpilot.auth.notification;

public interface AccountNotificationService {

  void sendPasswordResetLink(String email, String link);

  void sendEmailVerificationLink(String email, String link);
}
