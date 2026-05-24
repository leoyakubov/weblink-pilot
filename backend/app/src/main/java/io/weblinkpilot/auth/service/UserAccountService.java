package io.weblinkpilot.auth.service;

import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.domain.UserAccount;
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

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    public UserAccountService(UserAccountRepository repository,
                              PasswordEncoder passwordEncoder,
                              AuthProperties authProperties) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
    }

    @Transactional
    public UserAccount registerUser(String username, String rawPassword) {
        String normalizedUsername = normalizeUsername(username);
        if (repository.existsByUsername(normalizedUsername)) {
            throw new UsernameAlreadyExistsException(normalizedUsername);
        }

        UserAccount account = new UserAccount(
                normalizedUsername,
                passwordEncoder.encode(rawPassword),
                "USER",
                true,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        return repository.save(account);
    }

    @Transactional
    public UserAccount authenticate(String username, String rawPassword) {
        String normalizedUsername = normalizeUsername(username);
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
                        authProperties.getBootstrapAdminRole() == null || authProperties.getBootstrapAdminRole().isBlank()
                                ? "ADMIN"
                                : authProperties.getBootstrapAdminRole().trim().toUpperCase(Locale.ROOT),
                        true,
                        OffsetDateTime.now(ZoneOffset.UTC)
                )));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse profile(String username) {
        UserAccount account = repository.findByUsername(normalizeUsername(username))
                .orElseThrow(InvalidCredentialsException::new);
        return new UserProfileResponse(account.getUsername(), account.getRole());
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
}
