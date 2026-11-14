package com.societix.platform.society.api;

import com.societix.platform.error.ForbiddenException;
import com.societix.platform.error.UnauthenticatedException;
import com.societix.platform.society.internal.SocietyOnboardingService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/societies")
public class SocietyController {

    private final SocietyOnboardingService onboarding;

    public SocietyController(SocietyOnboardingService onboarding) {
        this.onboarding = onboarding;
    }

    @PostMapping
    public ResponseEntity<SocietyResponse> create(
            @RequestHeader(value = "X-Admin-Role", required = false) String adminRole,
            @Valid @RequestBody CreateSocietyRequest request) {
        requirePlatformAdmin(adminRole);
        SocietyResponse response = SocietyResponse.from(onboarding.onboard(request));
        return ResponseEntity.created(URI.create("/api/v1/societies/" + response.id())).body(response);
    }

    private void requirePlatformAdmin(String adminRole) {
        if (adminRole == null || adminRole.isBlank()) {
            throw new UnauthenticatedException();
        }
        if (!"PLATFORM_ADMIN".equals(adminRole)) {
            throw new ForbiddenException();
        }
    }
}
