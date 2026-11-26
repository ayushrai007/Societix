package com.societix.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

/** Redis-backed limits for Firebase-authenticated onboarding endpoints. */
public class OnboardingRateLimitFilter extends OncePerRequestFilter {

    private static final String MEMBERSHIP_REQUEST_PATH = "/api/v1/onboarding/membership-requests";
    private static final String ONBOARDING_PATH_PREFIX = "/api/v1/onboarding/";
    private static final Duration IP_WINDOW = Duration.ofMinutes(1);
    private static final Duration USER_WINDOW = Duration.ofHours(1);
    private static final DefaultRedisScript<Long> INCREMENT_WITH_TTL = new DefaultRedisScript<>(
            "local n = redis.call('INCR', KEYS[1]); "
                    + "if n == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]); end; "
                    + "return n",
            Long.class);

    private final StringRedisTemplate redis;
    private final int onboardingIpLimit;
    private final int membershipRequestUserLimit;

    public OnboardingRateLimitFilter(
            StringRedisTemplate redis, int onboardingIpLimit, int membershipRequestUserLimit) {
        if (onboardingIpLimit < 1 || membershipRequestUserLimit < 1) {
            throw new IllegalArgumentException("Onboarding rate limits must be positive");
        }
        this.redis = redis;
        this.onboardingIpLimit = onboardingIpLimit;
        this.membershipRequestUserLimit = membershipRequestUserLimit;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(ONBOARDING_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (!withinLimit("ip", request.getRemoteAddr(), onboardingIpLimit, IP_WINDOW)) {
                response.setHeader("Retry-After", Long.toString(IP_WINDOW.toSeconds()));
                response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too many onboarding requests");
                return;
            }
            if (request.getRequestURI().equals(MEMBERSHIP_REQUEST_PATH)
                    && "POST".equals(request.getMethod())
                    && !withinLimit("uid", jwt.getSubject(), membershipRequestUserLimit, USER_WINDOW)) {
                response.setHeader("Retry-After", Long.toString(USER_WINDOW.toSeconds()));
                response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too many membership requests");
                return;
            }
        } catch (DataAccessException exception) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Onboarding is temporarily unavailable");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean withinLimit(String keyType, String identifier, int limit, Duration window) {
        Long requests = redis.execute(
                INCREMENT_WITH_TTL,
                List.of("societix:ratelimit:onboarding:" + keyType + ":" + digest(identifier)),
                Long.toString(window.toMillis()));
        if (requests == null) {
            return false;
        }
        return requests <= limit;
    }

    private String digest(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
