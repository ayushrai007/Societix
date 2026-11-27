package com.societix.platform.society.api;

import com.societix.platform.society.internal.SocietyOnboardingService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<SocietyResponse> create(
            @Valid @RequestBody CreateSocietyRequest request) {
        SocietyResponse response = SocietyResponse.from(onboarding.onboard(request));
        return ResponseEntity.created(URI.create("/api/v1/societies/" + response.id())).body(response);
    }
}
