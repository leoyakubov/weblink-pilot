package io.weblinkpilot.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "app_users")
public class UserAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "username", nullable = false, unique = true, length = 128)
  private String username;

  @Column(name = "email", unique = true, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "role_id", nullable = false)
  private Role role;

  @Column(name = "enabled", nullable = false)
  private boolean enabled;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "email_verified_at")
  private OffsetDateTime emailVerifiedAt;

  @Column(name = "last_login_at")
  private OffsetDateTime lastLoginAt;

  protected UserAccount() {}

  public UserAccount(
      String username, String passwordHash, Role role, boolean enabled, OffsetDateTime createdAt) {
    this(username, passwordHash, null, role, enabled, createdAt, null);
  }

  public UserAccount(
      String username,
      String passwordHash,
      String email,
      Role role,
      boolean enabled,
      OffsetDateTime createdAt,
      OffsetDateTime emailVerifiedAt) {
    this.username = username;
    this.email = email;
    this.passwordHash = passwordHash;
    this.role = role;
    this.enabled = enabled;
    this.createdAt = createdAt;
    this.emailVerifiedAt = emailVerifiedAt;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String username;
    private String passwordHash;
    private String email;
    private Role role;
    private boolean enabled = true;
    private OffsetDateTime createdAt;
    private OffsetDateTime emailVerifiedAt;

    public Builder username(String username) {
      this.username = username;
      return this;
    }

    public Builder passwordHash(String passwordHash) {
      this.passwordHash = passwordHash;
      return this;
    }

    public Builder email(String email) {
      this.email = email;
      return this;
    }

    public Builder role(Role role) {
      this.role = role;
      return this;
    }

    public Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public Builder createdAt(OffsetDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder emailVerifiedAt(OffsetDateTime emailVerifiedAt) {
      this.emailVerifiedAt = emailVerifiedAt;
      return this;
    }

    public UserAccount build() {
      return new UserAccount(
          username, passwordHash, email, role, enabled, createdAt, emailVerifiedAt);
    }
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public Role getRole() {
    return role;
  }

  public String getRoleName() {
    return role == null ? null : role.getName();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getEmailVerifiedAt() {
    return emailVerifiedAt;
  }

  public void setEmailVerifiedAt(OffsetDateTime emailVerifiedAt) {
    this.emailVerifiedAt = emailVerifiedAt;
  }

  public boolean isEmailVerified() {
    return emailVerifiedAt != null;
  }

  public OffsetDateTime getLastLoginAt() {
    return lastLoginAt;
  }

  public void markLoggedIn(OffsetDateTime at) {
    this.lastLoginAt = at;
  }

  public void markEmailVerified(OffsetDateTime at) {
    this.emailVerifiedAt = at;
  }
}
