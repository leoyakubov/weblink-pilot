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
import java.time.OffsetDateTime;

@Entity
@Table(name = "app_account_action_tokens")
public class AccountActionToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "token_hash", nullable = false, unique = true, length = 128)
  private String tokenHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 32)
  private AccountActionTokenType type;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "expires_at", nullable = false)
  private OffsetDateTime expiresAt;

  @Column(name = "consumed_at")
  private OffsetDateTime consumedAt;

  protected AccountActionToken() {}

  @SuppressFBWarnings(value = "EI_EXPOSE_REP2")
  public AccountActionToken(
      String tokenHash,
      AccountActionTokenType type,
      UserAccount user,
      OffsetDateTime createdAt,
      OffsetDateTime expiresAt) {
    this.tokenHash = tokenHash;
    this.type = type;
    this.user = user;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }

  public Long getId() {
    return id;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public AccountActionTokenType getType() {
    return type;
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP")
  public UserAccount getUser() {
    return user;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  public OffsetDateTime getConsumedAt() {
    return consumedAt;
  }

  public boolean isActive(OffsetDateTime now) {
    return consumedAt == null && expiresAt.isAfter(now);
  }

  public void consume(OffsetDateTime at) {
    this.consumedAt = at;
  }
}
