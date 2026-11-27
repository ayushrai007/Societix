package com.societix.platform.security;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.societix.platform.identity.domain.Role;
import com.societix.platform.identity.internal.MembershipAuthorizationService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = TreasurerTestController.class,
        properties = {
            "societix.security.firebase.project-id=societix-test",
            "societix.security.firebase.jwk-set-uri=https://example.test/jwks"
        })
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
@Import({
    FirebaseSecurityConfiguration.class,
    FirebaseJwtDecoderConfiguration.class,
    SecurityProblemResponseHandler.class
})
class RoleAuthorizationTest {

    private static final UUID USER_ID = UUID.fromString("9f6596ca-74d2-48cc-9090-43f17d28ed54");
    private static final UUID TENANT_ID = UUID.fromString("825bf83f-2189-43f2-b089-6fb275112d45");

    private final MockMvc mockMvc;

    @MockitoBean
    private MembershipAuthorizationService authorizationService;

    @MockitoBean
    private StringRedisTemplate redis;

    @Autowired
    RoleAuthorizationTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void ownerCannotUseTreasurerOperationBySupplyingRoleHeaderAndBody() throws Exception {
        when(authorizationService.hasCurrentApprovedMembership("owner", USER_ID, TENANT_ID, 1))
                .thenReturn(true);

        mockMvc.perform(post("/api/v1/test/treasurer")
                        .header("X-Admin-Role", Role.TREASURER.name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"TREASURER\"]}")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("owner")
                                        .claim("userId", USER_ID.toString())
                                        .claim("tenantId", TENANT_ID.toString())
                                        .claim("authorizationVersion", 1L)
                                        .claim("roles", List.of(Role.OWNER.name())))
                                .authorities(new SimpleGrantedAuthority(Role.OWNER.authority()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void treasurerClaimCanUseTreasurerOperation() throws Exception {
        when(authorizationService.hasCurrentApprovedMembership("treasurer", USER_ID, TENANT_ID, 1))
                .thenReturn(true);

        mockMvc.perform(post("/api/v1/test/treasurer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"TREASURER\"]}")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("treasurer")
                                        .claim("userId", USER_ID.toString())
                                        .claim("tenantId", TENANT_ID.toString())
                                        .claim("authorizationVersion", 1L)
                                        .claim("roles", List.of(Role.TREASURER.name())))
                                .authorities(new SimpleGrantedAuthority(Role.TREASURER.authority()))))
                .andExpect(status().isOk());
    }

}
