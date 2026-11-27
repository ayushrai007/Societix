package com.societix.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.societix.platform.identity.internal.MembershipAuthorizationService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class MembershipAuthorizationFilterTest {

    private static final String FIREBASE_UID = "firebase-admin-uid";
    private static final UUID SOCIETIX_USER_ID = UUID.fromString("9f6596ca-74d2-48cc-9090-43f17d28ed54");
    private static final UUID SOCIETY_ID = UUID.fromString("825bf83f-2189-43f2-b089-6fb275112d45");
    private static final long AUTHORIZATION_VERSION = 7;

    private final MembershipAuthorizationService memberships = org.mockito.Mockito.mock(MembershipAuthorizationService.class);
    private final MembershipAuthorizationFilter filter = new MembershipAuthorizationFilter(memberships);

    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void cleanSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anonymousRequestsPassThroughToPublicEndpointAuthorization() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "test-key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(memberships);
    }

    @Test
    void currentPlatformAdminPassesWhenDatabaseStateAndTokenVersionMatch() throws Exception {
        when(memberships.hasActivePlatformAdmin(FIREBASE_UID, SOCIETIX_USER_ID, AUTHORIZATION_VERSION))
                .thenReturn(true);
        MockHttpServletRequest request = requestWithPlatformAdminToken();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
        verify(memberships).hasActivePlatformAdmin(FIREBASE_UID, SOCIETIX_USER_ID, AUTHORIZATION_VERSION);
    }

    @Test
    void suspendedDemotedOrStalePlatformAdminIsDeniedWhenDatabaseCheckFails() throws Exception {
        when(memberships.hasActivePlatformAdmin(FIREBASE_UID, SOCIETIX_USER_ID, AUTHORIZATION_VERSION))
                .thenReturn(false);
        MockHttpServletRequest request = requestWithPlatformAdminToken();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(chain.getRequest()).isNull();
        verify(memberships).hasActivePlatformAdmin(FIREBASE_UID, SOCIETIX_USER_ID, AUTHORIZATION_VERSION);
    }

    @Test
    void mixedPlatformAdminAndSocietyRoleRequiresBothCurrentVersions() throws Exception {
        when(memberships.hasActivePlatformAdmin(FIREBASE_UID, SOCIETIX_USER_ID, AUTHORIZATION_VERSION))
                .thenReturn(true);
        when(memberships.hasCurrentApprovedMembership(FIREBASE_UID, SOCIETIX_USER_ID, SOCIETY_ID, 3))
                .thenReturn(false);
        MockHttpServletRequest request = requestWithMixedRoleToken();

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain()))
                .isInstanceOf(AccessDeniedException.class);

        verify(memberships).hasActivePlatformAdmin(FIREBASE_UID, SOCIETIX_USER_ID, AUTHORIZATION_VERSION);
        verify(memberships).hasCurrentApprovedMembership(FIREBASE_UID, SOCIETIX_USER_ID, SOCIETY_ID, 3);
    }

    @Test
    void authenticatedHealthRequestsDoNotDependOnSocietyMembership() throws Exception {
        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(FIREBASE_UID)
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_OWNER"))));

        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest("GET", "/health"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(memberships);
    }

    private MockHttpServletRequest requestWithMixedRoleToken() {
        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(FIREBASE_UID)
                .claim("userId", SOCIETIX_USER_ID.toString())
                .claim("tenantId", SOCIETY_ID.toString())
                .claim("authorizationVersion", AUTHORIZATION_VERSION)
                .claim("membershipAuthorizationVersion", 3)
                .claim("roles", List.of("PLATFORM_ADMIN", "SECRETARY"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt,
                List.of(
                        new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_SECRETARY"))));
        return new MockHttpServletRequest("GET", "/api/v1/membership-requests/pending");
    }

    private MockHttpServletRequest requestWithPlatformAdminToken() {
        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(FIREBASE_UID)
                .claim("userId", SOCIETIX_USER_ID.toString())
                .claim("authorizationVersion", AUTHORIZATION_VERSION)
                .claim("roles", List.of("PLATFORM_ADMIN"))
                .build();
        var authentication = new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return new MockHttpServletRequest("GET", "/api/v1/societies");
    }
}
