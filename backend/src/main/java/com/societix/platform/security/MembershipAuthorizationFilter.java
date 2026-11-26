package com.societix.platform.security;

import com.societix.platform.identity.domain.SocietixRoleClaims;
import com.societix.platform.identity.internal.MembershipAuthorizationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

/** Checks every tenant API request against current membership state and token authorization version. */
public class MembershipAuthorizationFilter extends OncePerRequestFilter {

    private static final String ONBOARDING_PREFIX = "/api/v1/onboarding/";
    private static final String HEALTH_PATH = "/health";
    private static final String ACTUATOR_HEALTH_PATH = "/actuator/health";
    private final MembershipAuthorizationService memberships;

    public MembershipAuthorizationFilter(MembershipAuthorizationService memberships) {
        this.memberships = memberships;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith(ONBOARDING_PREFIX)
                || HEALTH_PATH.equals(path)
                || ACTUATOR_HEALTH_PATH.equals(path)
                || path.startsWith(ACTUATOR_HEALTH_PATH + "/")) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            boolean platformAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority()
                            .equals("ROLE_" + SocietixRoleClaims.PLATFORM_ADMIN));
            boolean societyRole = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().startsWith("ROLE_")
                            && !authority.getAuthority().equals("ROLE_" + SocietixRoleClaims.PLATFORM_ADMIN));
            String userIdClaim = jwt.getClaimAsString("userId");
            Object versionClaim = jwt.getClaim("authorizationVersion");
            if (userIdClaim == null || !(versionClaim instanceof Number version)) {
                deny(response);
                return;
            }
            UUID userId = UUID.fromString(userIdClaim);
            boolean current = true;
            if (platformAdmin) {
                current = memberships.hasActivePlatformAdmin(jwt.getSubject(), userId, version.longValue());
            }

            String tenantIdClaim = jwt.getClaimAsString("tenantId");
            boolean societyContext = tenantIdClaim != null || societyRole;
            if (societyContext) {
                if (tenantIdClaim == null) {
                    deny(response);
                    return;
                }
                Object membershipVersionClaim = jwt.getClaim("membershipAuthorizationVersion");
                if (platformAdmin && !(membershipVersionClaim instanceof Number)) {
                    deny(response);
                    return;
                }
                long membershipVersion = membershipVersionClaim instanceof Number membershipVersionNumber
                        ? membershipVersionNumber.longValue()
                        : version.longValue();
                current = current && memberships.hasCurrentApprovedMembership(
                        jwt.getSubject(), userId, UUID.fromString(tenantIdClaim), membershipVersion);
            } else if (!platformAdmin) {
                deny(response);
                return;
            }
            if (!current) {
                deny(response);
                return;
            }
        } catch (IllegalArgumentException exception) {
            deny(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void deny(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Account approval or token refresh is required");
    }
}
