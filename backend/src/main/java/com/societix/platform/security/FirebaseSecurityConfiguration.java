package com.societix.platform.security;

import com.societix.platform.identity.domain.Role;
import com.societix.platform.identity.internal.MembershipAuthorizationService;
import com.societix.platform.error.ProblemResponseWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;

@Configuration
@EnableMethodSecurity
public class FirebaseSecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtDecoder firebaseJwtDecoder,
            Converter<Jwt, AbstractAuthenticationToken> converter,
            SecurityProblemResponseHandler securityProblemResponseHandler,
            ProblemResponseWriter problemResponseWriter,
            MembershipAuthorizationService authorizationService,
            StringRedisTemplate redis,
            @Value("${societix.security.rate-limit.onboarding-requests-per-ip-per-minute:60}") int onboardingIpLimit,
            @Value("${societix.security.rate-limit.membership-requests-per-user-per-hour:5}")
                    int membershipRequestUserLimit,
            @Value("${societix.security.society-creation-enabled:false}") boolean societyCreationEnabled)
            throws Exception {
        return http.authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers("/health", "/actuator/health").permitAll();
                    if (societyCreationEnabled) {
                        authorize.requestMatchers("/actuator/metrics/**").hasRole(Role.PLATFORM_ADMIN.name());
                        authorize.requestMatchers(HttpMethod.POST, "/api/v1/societies")
                                .hasRole(Role.PLATFORM_ADMIN.name());
                    } else {
                        authorize.requestMatchers("/actuator/metrics/**").denyAll();
                    }
                    authorize.requestMatchers("/actuator/**").denyAll();
                    if (societyCreationEnabled) {
                        authorize.requestMatchers(HttpMethod.POST, "/api/v1/societies")
                                .hasRole(Role.PLATFORM_ADMIN.name());
                    } else {
                        authorize.requestMatchers(HttpMethod.POST, "/api/v1/societies").denyAll();
                    }
                    authorize.anyRequest().authenticated();
                })
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .authenticationEntryPoint(securityProblemResponseHandler)
                        .jwt(jwt -> jwt.decoder(firebaseJwtDecoder).jwtAuthenticationConverter(converter)))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(securityProblemResponseHandler)
                        .accessDeniedHandler(securityProblemResponseHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer::disable)
                .formLogin(org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer::disable)
                .csrf(org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer::disable)
                .addFilterAfter(new MembershipAuthorizationFilter(authorizationService), ExceptionTranslationFilter.class)
                .addFilterAfter(
                        new OnboardingRateLimitFilter(
                                redis, problemResponseWriter, onboardingIpLimit, membershipRequestUserLimit),
                        MembershipAuthorizationFilter.class)
                .build();
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> firebaseJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new FirebaseRoleAuthoritiesConverter());
        return converter;
    }
}
