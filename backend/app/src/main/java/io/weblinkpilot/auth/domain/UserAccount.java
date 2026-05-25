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

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "role_id", nullable = false)
  private Role role;

  @Column(name = "enabled", nullable = false)
  private boolean enabled;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "last_login_at")
  private OffsetDateTime lastLoginAt;

  protected UserAccount() {}

  public UserAccount(
      String username, String passwordHash, Role role, boolean enabled, OffsetDateTime createdAt) {
    this.username = username;
    this.passwordHash = passwordHash;
    this.role = role;
    this.enabled = enabled;
    this.createdAt = createdAt;
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

  public OffsetDateTime getLastLoginAt() {
    return lastLoginAt;
  }

  public void markLoggedIn(OffsetDateTime at) {
    this.lastLoginAt = at;
  }
}
