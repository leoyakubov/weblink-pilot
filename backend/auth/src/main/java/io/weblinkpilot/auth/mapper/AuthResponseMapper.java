package io.weblinkpilot.auth.mapper;

import io.weblinkpilot.auth.domain.SocialIdentity;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.shared.api.admin.AdminUserResponse;
import io.weblinkpilot.shared.api.auth.AccountIdentityResponse;
import io.weblinkpilot.shared.api.auth.AccountProfileResponse;
import io.weblinkpilot.shared.api.links.LinkCreatorOptionResponse;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AuthResponseMapper {

  public AccountProfileResponse toProfile(
      UserAccount account, List<AccountIdentityResponse> identities) {
    return new AccountProfileResponse(
        account.getUsername(),
        account.getRoleName(),
        account.getEmail(),
        account.isEmailVerified(),
        toText(account.getCreatedAt()),
        toText(account.getLastLoginAt()),
        identities);
  }

  public AccountIdentityResponse toIdentity(SocialIdentity identity) {
    return new AccountIdentityResponse(identity.getProvider().name(), identity.getProviderLogin());
  }

  public LinkCreatorOptionResponse toCreatorOption(UserAccount account) {
    return new LinkCreatorOptionResponse(account.getUsername(), account.getRoleName());
  }

  public AdminUserResponse toAdminUser(UserAccount account) {
    return new AdminUserResponse(
        account.getUsername(),
        account.getEmail(),
        account.getRoleName(),
        account.isEnabled(),
        account.isEmailVerified(),
        account.getCreatedAt(),
        account.getLastLoginAt());
  }

  private String toText(OffsetDateTime value) {
    return value == null ? null : value.toString();
  }
}
