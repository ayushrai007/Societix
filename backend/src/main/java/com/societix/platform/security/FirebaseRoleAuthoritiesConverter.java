package com.societix.platform.security;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** Converts only declared Societix roles; SOC-12 will replace this allowlist with its Role enum. */
final class FirebaseRoleAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final Set<String> DECLARED_ROLES = Set.of(
            "PLATFORM_ADMIN",
            "SECRETARY",
            "TREASURER",
            "COMMITTEE_MEMBER",
            "OWNER",
            "TENANT",
            "GUARD",
            "AUDITOR");

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object claim = jwt.getClaim("roles");
        if (!(claim instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(DECLARED_ROLES::contains)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }
}
