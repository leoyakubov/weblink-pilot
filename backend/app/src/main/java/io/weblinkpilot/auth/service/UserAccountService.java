package io.weblinkpilot.auth.service;

import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.config.RoleNames;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.domain.Role;
import io.weblinkpilot.auth.exception.AccountDisabledException;
import io.weblinkpilot.auth.exception.InvalidCredentialsException;
import io.weblinkpilot.auth.exception.UsernameAlreadyExistsException;
import io.weblinkpilot.auth.repository.UserAccountRepository;
import io.weblinkpilot.shared.contracts.UserProfileResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

    private static final String USERNAME_PATTERN = "^(?=.*[A-Za-z])[A-Za-z0-9]{4,}$";
    private static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)\\S{6,}$";

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;
    private final RoleCatalogService roleCatalogService;

    public UserAccountService(UserAccountRepository repository,
                              PasswordEncoder passwordEncoder,
                              AuthProperties authProperties,
                              RoleCatalogService roleCatalogService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
        this.roleCatalogService = roleCatalogService;
    }

    @Transactional
    public UserAccount registerUser(String username, String rawPassword) {
        String normalizedUsername = normalizeUsername(username);
        validateSignupCredentials(normalizedUsername, rawPassword);
        if (repository.existsByUsername(normalizedUsername)) {
            throw new UsernameAlreadyExistsException(normalizedUsername);
        }

        Role userRole = roleCatalogService.getRequiredRole(RoleNames.USER);
        UserAccount account = new UserAccount(
                normalizedUsername,
                passwordEncoder.encode(rawPassword),
                userRole,
                true,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        return repository.save(account);
    }

    @Transactional
    public UserAccount authenticate(String username, String rawPassword) {
        String normalizedUsername = normalizeUsername(username);
        validateLoginCredentials(normalizedUsername, rawPassword);
        UserAccount account = repository.findByUsername(normalizedUsername)
                .orElseThrow(InvalidCredentialsException::new);

        if (!account.isEnabled()) {
            throw new AccountDisabledException(normalizedUsername);
        }
        if (!passwordEncoder.matches(rawPassword, account.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        account.markLoggedIn(OffsetDateTime.now(ZoneOffset.UTC));
        return repository.save(account);
    }

    @Transactional
    public UserAccount ensureBootstrapAdmin() {
        String username = normalizeUsername(authProperties.getBootstrapAdminUsername());
        String password = authProperties.getBootstrapAdminPassword();
        if (username.isBlank() || password == null || password.isBlank()) {
            return null;
        }

        return repository.findByUsername(username)
                .orElseGet(() -> repository.save(new UserAccount(
                        username,
                        passwordEncoder.encode(password),
                        roleCatalogService.getRequiredRole(normalizeBootstrapRole(
                                authProperties.getBootstrapAdminRole(),
                                RoleNames.ADMIN
                        )),
                        true,
                        OffsetDateTime.now(ZoneOffset.UTC)
                )));
    }

    @Transactional
    public UserAccount ensureBootstrapUser() {
        String username = normalizeUsername(authProperties.getBootstrapUserUsername());
        String password = authProperties.getBootstrapUserPassword();
        if (username.isBlank() || password == null || password.isBlank()) {
            return null;
        }

        return repository.findByUsername(username)
                .orElseGet(() -> repository.save(new UserAccount(
                        username,
                        passwordEncoder.encode(password),
                        roleCatalogService.getRequiredRole(normalizeBootstrapRole(
                                authProperties.getBootstrapUserRole(),
                                RoleNames.USER
                        )),
                        true,
                        OffsetDateTime.now(ZoneOffset.UTC)
                )));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse profile(String username) {
        UserAccount account = repository.findByUsername(normalizeUsername(username))
                .orElseThrow(InvalidCredentialsException::new);
        return new UserProfileResponse(account.getUsername(), account.getRoleName());
    }

    @Transactional(readOnly = true)
    public UserAccount getRequiredUser(String username) {
        return repository.findByUsername(normalizeUsername(username))
                .orElseThrow(InvalidCredentialsException::new);
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeBootstrapRole(String configuredRole, String defaultRole) {
        if (configuredRole == null || configuredRole.isBlank()) {
            return defaultRole;
        }
        return configuredRole.trim().toUpperCase(Locale.ROOT);
    }

    private void validateSignupCredentials(String username, String password) {
        if (username.isBlank() && (password == null || password.isBlank())) {
            throw new IllegalArgumentException("Enter both username and password.");
        }
        if (username.isBlank()) {
            throw new IllegalArgumentException("Username must use at least 4 symbols.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must use at least 6 characters, including 1 letter and 1 number.");
        }
        if (!username.matches(USERNAME_PATTERN)) {
            throw new IllegalArgumentException("Username must use at least 4 symbols.");
        }
        if (!password.matches(PASSWORD_PATTERN)) {
            throw new IllegalArgumentException("Password must use at least 6 characters, including 1 letter and 1 number.");
        }
    }

    private void validateLoginCredentials(String username, String password) {
        if (username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Enter both username and password.");
        }
    }
}
