package com.societix.platform.identity.api;

import com.societix.platform.identity.internal.IdentityContactService;
import com.societix.platform.identity.internal.MembershipService;
import com.societix.platform.identity.domain.PhoneNumberProtector;
import jakarta.validation.Valid;
import java.util.UUID;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MembershipController {

    private final MembershipService memberships;
    private final IdentityContactService identityContacts;
    private final PhoneNumberProtector phoneNumberProtector;

    public MembershipController(
            MembershipService memberships,
            IdentityContactService identityContacts,
            PhoneNumberProtector phoneNumberProtector) {
        this.memberships = memberships;
        this.identityContacts = identityContacts;
        this.phoneNumberProtector = phoneNumberProtector;
    }

    @PostMapping("/onboarding/membership-requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MembershipResponse requestMembership(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MembershipRequestBody request) {
        requirePhoneAuthentication(jwt);
        return memberships.requestMembership(
                jwt.getSubject(),
                verifiedPhoneNumber(jwt),
                request.societyId(),
                request.membershipType());
    }

    @GetMapping("/onboarding/status")
    public OnboardingStatusResponse onboardingStatus(
            @AuthenticationPrincipal Jwt jwt, @RequestParam UUID societyId) {
        return memberships.onboardingStatus(jwt.getSubject(), societyId, authorizationVersion(jwt));
    }

    @PutMapping("/me/contact-phone")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePhoneNumber(@AuthenticationPrincipal Jwt jwt) {
        identityContacts.updateVerifiedPhone(jwt.getSubject(), verifiedPhoneNumber(jwt));
    }

    private String verifiedPhoneNumber(Jwt jwt) {
        String phoneNumber = jwt.getClaimAsString("phone_number");
        if (!phoneNumberProtector.supportsPhoneNumber(phoneNumber)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A Firebase-verified Indian phone number in E.164 format is required");
        }
        return phoneNumber;
    }

    private void requirePhoneAuthentication(Jwt jwt) {
        Object firebaseClaim = jwt.getClaim("firebase");
        if (!(firebaseClaim instanceof Map<?, ?> firebase)
                || !"phone".equals(firebase.get("sign_in_provider"))) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Membership requests require Firebase Phone Authentication");
        }
    }

    private Long authorizationVersion(Jwt jwt) {
        Object claim = jwt.getClaim("authorizationVersion");
        return claim instanceof Number version ? version.longValue() : null;
    }
}
