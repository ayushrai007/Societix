package com.societix.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
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
}
