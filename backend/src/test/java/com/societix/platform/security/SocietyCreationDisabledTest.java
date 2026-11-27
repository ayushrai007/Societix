package com.societix.platform.security;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.societix.platform.society.internal.SocietyOnboardingService;
import com.societix.platform.society.api.SocietyController;
import com.societix.platform.identity.internal.MembershipAuthorizationService;
import com.societix.platform.error.ProblemResponseWriter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;

@WebMvcTest(
        controllers = SocietyController.class,
        properties = {
            "societix.security.society-creation-enabled=false",
            "societix.security.firebase.project-id=societix-test",
            "societix.security.firebase.jwk-set-uri=https://example.test/jwks"
        })
@ImportAutoConfiguration({
    SecurityAutoConfiguration.class,
    ServletWebSecurityAutoConfiguration.class
})
@Import({
    FirebaseSecurityConfiguration.class,
    FirebaseJwtDecoderConfiguration.class,
    ProblemResponseWriter.class,
    SecurityProblemResponseHandler.class
})
class SocietyCreationDisabledTest {

    private static final UUID PLATFORM_ADMIN_ID = UUID.fromString("8db74fbb-b015-40d4-818c-0f4ab3524e1b");
    private static final String PLATFORM_ADMIN_UID = "societix-test-platform-admin";

    private final MockMvc mockMvc;

    @MockitoBean
    private SocietyOnboardingService onboarding;

    @MockitoBean
    private MembershipAuthorizationService authorizationService;

    @MockitoBean
    private StringRedisTemplate redis;

    @Autowired
    SocietyCreationDisabledTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void activePlatformAdminCannotCreateSocietyWhileFeatureIsDisabled() throws Exception {
        when(authorizationService.hasActivePlatformAdmin(PLATFORM_ADMIN_UID, PLATFORM_ADMIN_ID, 1))
                .thenReturn(true);
        mockMvc.perform(post("/api/v1/societies")
                        .with(jwt().jwt(token -> token
                                        .subject(PLATFORM_ADMIN_UID)
                                        .claim("userId", PLATFORM_ADMIN_ID.toString())
                                        .claim("authorizationVersion", 1)
                                        .claim("roles", List.of("PLATFORM_ADMIN")))
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "ROLE_PLATFORM_ADMIN")))
                        .contentType("application/json")
                        .content("""
                                {"name":"Test Society","address":"1 Test Road","state":"MH"}
                                """))
                .andExpect(status().isForbidden());
    }
}
