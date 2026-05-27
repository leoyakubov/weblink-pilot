package io.weblinkpilot.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.config.RoleNames;
import io.weblinkpilot.auth.domain.Role;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.exception.AccountDisabledException;
import io.weblinkpilot.auth.exception.EmailNotVerifiedException;
import io.weblinkpilot.auth.exception.InvalidCredentialsException;
import io.weblinkpilot.auth.exception.UsernameAlreadyExistsException;
import io.weblinkpilot.auth.repository.UserAccountRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

  @Mock private UserAccountRepository repository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private RoleCatalogService roleCatalogService;

  private AuthProperties authProperties;
  private UserAccountService service;

  @BeforeEach
  void setUp() {
    authProperties = new AuthProperties();
    authProperties.setBootstrapAdminUsername("admin");
    authProperties.setBootstrapAdminPassword("admin123");
    authProperties.setBootstrapAdminRole(RoleNames.ADMIN);
    authProperties.setBootstrapUserUsername("user");
    authProperties.setBootstrapUserPassword("user123");
    authProperties.setBootstrapUserRole(RoleNames.USER);
    service =
        new UserAccountService(repository, passwordEncoder, authProperties, roleCatalogService);
  }

  @Test
  void registerUserStoresEncodedAccount() {
    when(repository.existsByUsername("alice")).thenReturn(false);
    when(repository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
    when(roleCatalogService.getRequiredRole(RoleNames.USER)).thenReturn(new Role(RoleNames.USER));
    when(passwordEncoder.encode("Password1")).thenReturn("hashed");
    when(repository.save(any(UserAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UserAccount account = service.registerUser(" Alice ", "Password1", "alice@example.com");

    assertThat(account.getUsername()).isEqualTo("alice");
    assertThat(account.getEmail()).isEqualTo("alice@example.com");
    assertThat(account.getPasswordHash()).isEqualTo("hashed");
    assertThat(account.getRoleName()).isEqualTo(RoleNames.USER);
    assertThat(account.isEnabled()).isTrue();
    assertThat(account.getCreatedAt()).isNotNull();
    assertThat(account.isEmailVerified()).isFalse();
    verify(repository).save(any(UserAccount.class));
  }

  @Test
  void registerUserRejectsDuplicateUsername() {
    when(repository.existsByUsername("alice")).thenReturn(true);

    assertThatThrownBy(() -> service.registerUser("alice", "Password1", "alice@example.com"))
        .isInstanceOf(UsernameAlreadyExistsException.class);
  }

  @Test
  void registerUserRejectsDuplicateEmail() {
    when(repository.existsByUsername("alice")).thenReturn(false);
    when(repository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

    assertThatThrownBy(() -> service.registerUser("alice", "Password1", "alice@example.com"))
        .isInstanceOf(io.weblinkpilot.auth.exception.EmailAlreadyExistsException.class);
  }

  @Test
  void authenticateUpdatesLastLoginAt() {
    UserAccount account =
        new UserAccount(
            "alice", "hashed", new Role(RoleNames.USER), true, OffsetDateTime.now(ZoneOffset.UTC));
    account.setEmailVerifiedAt(OffsetDateTime.now(ZoneOffset.UTC));
    when(repository.findByUsername("alice")).thenReturn(Optional.of(account));
    when(passwordEncoder.matches("Password1", "hashed")).thenReturn(true);
    when(repository.save(any(UserAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UserAccount authenticated = service.authenticate(" Alice ", "Password1");

    assertThat(authenticated.getLastLoginAt()).isNotNull();
    assertThat(authenticated.getUsername()).isEqualTo("alice");
    verify(repository).save(account);
  }

  @Test
  void authenticateRejectsDisabledAccount() {
    UserAccount account =
        new UserAccount(
            "alice", "hashed", new Role(RoleNames.USER), false, OffsetDateTime.now(ZoneOffset.UTC));
    account.setEmailVerifiedAt(OffsetDateTime.now(ZoneOffset.UTC));
    when(repository.findByUsername("alice")).thenReturn(Optional.of(account));

    assertThatThrownBy(() -> service.authenticate("alice", "Password1"))
        .isInstanceOf(AccountDisabledException.class)
        .hasMessageContaining("Incorrect username or password");
  }

  @Test
  void authenticateRejectsWrongPassword() {
    UserAccount account =
        new UserAccount(
            "alice", "hashed", new Role(RoleNames.USER), true, OffsetDateTime.now(ZoneOffset.UTC));
    account.setEmailVerifiedAt(OffsetDateTime.now(ZoneOffset.UTC));
    when(repository.findByUsername("alice")).thenReturn(Optional.of(account));
    when(passwordEncoder.matches("Password1", "hashed")).thenReturn(false);

    assertThatThrownBy(() -> service.authenticate("alice", "Password1"))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void authenticateRejectsUnverifiedEmail() {
    UserAccount account =
        new UserAccount(
            "alice",
            "hashed",
            "alice@example.com",
            new Role(RoleNames.USER),
            true,
            OffsetDateTime.now(ZoneOffset.UTC),
            null);
    when(repository.findByUsername("alice")).thenReturn(Optional.of(account));
    when(passwordEncoder.matches("Password1", "hashed")).thenReturn(true);

    assertThatThrownBy(() -> service.authenticate("alice", "Password1"))
        .isInstanceOf(EmailNotVerifiedException.class)
        .hasMessageContaining("Please verify your email address");
  }

  @Test
  void ensureBootstrapAdminReturnsNullWhenCredentialsMissing() {
    authProperties.setBootstrapAdminUsername(" ");
    authProperties.setBootstrapAdminPassword(" ");
    service =
        new UserAccountService(repository, passwordEncoder, authProperties, roleCatalogService);

    assertThat(service.ensureBootstrapAdmin()).isNull();
  }

  @Test
  void ensureBootstrapAdminCreatesAccountWithDefaultRole() {
    when(repository.findByUsername("admin")).thenReturn(Optional.empty());
    when(roleCatalogService.getRequiredRole(RoleNames.ADMIN)).thenReturn(new Role(RoleNames.ADMIN));
    when(passwordEncoder.encode("admin123")).thenReturn("encoded");
    when(repository.save(any(UserAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UserAccount account = service.ensureBootstrapAdmin();

    assertThat(account.getUsername()).isEqualTo("admin");
    assertThat(account.getRoleName()).isEqualTo(RoleNames.ADMIN);
    assertThat(account.isEnabled()).isTrue();
    assertThat(account.isEmailVerified()).isTrue();
  }

  @Test
  void ensureBootstrapUserNormalizesConfiguredRole() {
    authProperties.setBootstrapUserRole("  member  ");
    service =
        new UserAccountService(repository, passwordEncoder, authProperties, roleCatalogService);
    when(repository.findByUsername("user")).thenReturn(Optional.empty());
    when(roleCatalogService.getRequiredRole("MEMBER")).thenReturn(new Role("MEMBER"));
    when(passwordEncoder.encode("user123")).thenReturn("encoded");
    when(repository.save(any(UserAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UserAccount account = service.ensureBootstrapUser();

    assertThat(account.getUsername()).isEqualTo("user");
    assertThat(account.getRoleName()).isEqualTo("MEMBER");
    assertThat(account.isEmailVerified()).isTrue();
  }

  @Test
  void profileReturnsProjection() {
    UserAccount account =
        new UserAccount(
            "alice", "hashed", new Role(RoleNames.USER), true, OffsetDateTime.now(ZoneOffset.UTC));
    account.setEmailVerifiedAt(OffsetDateTime.now(ZoneOffset.UTC));
    when(repository.findByUsername("alice")).thenReturn(Optional.of(account));

    assertThat(service.profile(" Alice "))
        .isEqualTo(
            new io.weblinkpilot.shared.contracts.UserProfileResponse("alice", RoleNames.USER));
  }

  @Test
  void getRequiredUserRejectsMissingAccount() {
    when(repository.findByUsername("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getRequiredUser(" missing "))
        .isInstanceOf(InvalidCredentialsException.class);
  }
}
