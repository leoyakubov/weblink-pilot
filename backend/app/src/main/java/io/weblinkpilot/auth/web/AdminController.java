package io.weblinkpilot.auth.web;

import io.weblinkpilot.auth.repository.UserAccountRepository;
import io.weblinkpilot.shared.contracts.AdminOverviewResponse;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserAccountRepository userAccountRepository;
    private final ShortLinkRepository shortLinkRepository;

    public AdminController(UserAccountRepository userAccountRepository, ShortLinkRepository shortLinkRepository) {
        this.userAccountRepository = userAccountRepository;
        this.shortLinkRepository = shortLinkRepository;
    }

    @GetMapping("/overview")
    @Operation(summary = "Admin overview")
    @SecurityRequirement(name = "bearerAuth")
    public AdminOverviewResponse overview() {
        long totalUsers = userAccountRepository.count();
        long adminUsers = userAccountRepository.countByRole_Name("ADMIN");
        long totalLinks = shortLinkRepository.count();
        long anonymousLinks = shortLinkRepository.countByOwnerUsernameIsNull();
        long ownedLinks = shortLinkRepository.countByOwnerUsernameIsNotNull();
        long totalClicks = shortLinkRepository.sumClickCount();
        return new AdminOverviewResponse(
                totalUsers,
                adminUsers,
                totalLinks,
                anonymousLinks,
                ownedLinks,
                totalClicks
        );
    }
}
