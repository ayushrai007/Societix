package com.societix.platform.security;

import com.societix.platform.identity.domain.Role;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class FirebaseRoleAuthoritiesConverterTest {

    private final FirebaseRoleAuthoritiesConverter converter = new FirebaseRoleAuthoritiesConverter();

    @Test
    void mapsOnlyDeclaredSocietixRolesToAuthorities() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("firebase-uid")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("roles", List.of("PLATFORM_ADMIN", "SECRETARY", "ROOT_ADMIN", "ROLE_OWNER"))
                .build();

        assertThat(converter.convert(jwt))
                .extracting("authority")
                .containsExactly("ROLE_PLATFORM_ADMIN", "ROLE_SECRETARY");
    }

    @Test
    void everyDeclaredRoleMapsToItsMatchingAuthority() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("firebase-uid")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("roles", Arrays.stream(Role.values()).map(Enum::name).toList())
                .build();

        assertThat(converter.convert(jwt))
                .extracting("authority")
                .containsExactlyElementsOf(Arrays.stream(Role.values()).map(Role::authority).toList());
    }
}
