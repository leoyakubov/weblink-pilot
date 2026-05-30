package io.weblinkpilot.auth.domain;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "app_social_identities",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_social_identity_provider_user",
          columnNames = {"provider", "provider_user_id"}),
      @UniqueConstraint(
          name = "uk_social_identity_provider_account",
          columnNames = {"provider", "user_id"})
    })
public class SocialIdentity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 32)
  private SocialLoginProvider provider;

  @Column(name = "provider_user_id", nullable = false, length = 128)
  private String providerUserId;

  @Column(name = "provider_login", length = 128)
  private String providerLogin;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "last_login_at", nullable = false)
  private OffsetDateTime lastLoginAt;

  protected SocialIdentity() {}

  @SuppressFBWarnings(value = "EI_EXPOSE_REP2")
  public SocialIdentity(
      SocialLoginProvider provider,
      String providerUserId,
      String providerLogin,
      UserAccount user,
      OffsetDateTime createdAt,
      OffsetDateTime lastLoginAt) {
    this.provider = provider;
    this.providerUserId = providerUserId;
    this.providerLogin = providerLogin;
    this.user = user;
    this.createdAt = createdAt;
    this.lastLoginAt = lastLoginAt;
  }

  public Long getId() {
    return id;
  }

  public SocialLoginProvider getProvider() {
    return provider;
  }

  public String getProviderUserId() {
    return providerUserId;
  }

  public String getProviderLogin() {
    return providerLogin;
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP")
  public UserAccount getUser() {
    return user;
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
