package io.weblinkpilot.auth.service;

import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.shared.contracts.AuthCredentialsRequest;
import io.weblinkpilot.shared.contracts.AuthResponse;
import io.weblinkpilot.shared.contracts.UserProfileResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserAccountService userAccountService;
    private final JwtService jwtService;

    public AuthService(UserAccountService userAccountService, JwtService jwtService) {
        this.userAccountService = userAccountService;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(AuthCredentialsRequest request) {
        UserAccount account = userAccountService.registerUser(request.username(), request.password());
        return new AuthResponse(jwtService.issueToken(account.getUsername(), account.getRole()), account.getUsername(), account.getRole());
    }

    @Transactional
    public AuthResponse login(AuthCredentialsRequest request) {
        UserAccount account = userAccountService.authenticate(request.username(), request.password());
        return new AuthResponse(jwtService.issueToken(account.getUsername(), account.getRole()), account.getUsername(), account.getRole());
    }

    @Transactional(readOnly = true)
    public UserProfileResponse profile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }
        return userAccountService.profile(authentication.getName());
    }
}
