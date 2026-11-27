package com.societix.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.societix.platform.error.ProblemResponseWriter;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.ObjectMapper;

class OnboardingRateLimitFilterTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ProblemResponseWriter problemResponses = new ProblemResponseWriter(new ObjectMapper());
    private final OnboardingRateLimitFilter filter = new OnboardingRateLimitFilter(redis, problemResponses, 60, 5);

    @BeforeEach
    void setAuthenticatedUser() {
        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("firebase-user")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rateLimitResponseUsesProblemJsonAndRetryAfter() throws Exception {
        when(redis.execute(any(), anyList(), any())).thenReturn(61L);
        var request = new MockHttpServletRequest("POST", "/api/v1/onboarding/membership-requests");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response.getContentAsString()).contains("SOC-429-RATE-LIMITED", "Too Many Requests");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void redisFailureReturnsProblemJsonServiceUnavailable() throws Exception {
        when(redis.execute(any(), anyList(), any()))
                .thenThrow(new DataAccessResourceFailureException("Redis unavailable"));
        var request = new MockHttpServletRequest("POST", "/api/v1/onboarding/membership-requests");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getContentAsString()).contains("SOC-503-SERVICE-UNAVAILABLE");
        assertThat(chain.getRequest()).isNull();
    }
}
