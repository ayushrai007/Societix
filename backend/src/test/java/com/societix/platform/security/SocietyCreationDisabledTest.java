package com.societix.platform.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.societix.platform.society.internal.SocietyOnboardingService;
import com.societix.platform.society.api.SocietyController;
import com.societix.platform.identity.internal.MembershipAuthorizationService;
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
    SecurityProblemResponseHandler.class
})
class SocietyCreationDisabledTest {

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
    void platformAdminCannotCreateSocietyUntilCurrentAccountChecksAreAvailable() throws Exception {
        mockMvc.perform(post("/api/v1/societies")
                        .with(user("platform-admin").roles("PLATFORM_ADMIN"))
                        .contentType("application/json")
                        .content("""
                                {"name":"Test Society","address":"1 Test Road","state":"MH"}
                                """))
                .andExpect(status().isForbidden());
    }
}
