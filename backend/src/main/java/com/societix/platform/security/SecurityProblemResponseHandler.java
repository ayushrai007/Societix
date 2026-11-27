package com.societix.platform.security;

import com.societix.platform.error.ProblemResponseWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Renders Spring Security failures in the project's standard problem response format. */
@Component
final class SecurityProblemResponseHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ProblemResponseWriter problemResponses;
    private final BearerTokenAuthenticationEntryPoint bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();

    SecurityProblemResponseHandler(ProblemResponseWriter problemResponses) {
        this.problemResponses = problemResponses;
    }

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {
        bearerEntryPoint.commence(request, response, exception);
        problemResponses.write(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "https://errors.societix.dev/unauthenticated",
                "Unauthenticated",
                "A valid Firebase bearer token is required",
                "SOC-401-UNAUTHENTICATED");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        problemResponses.write(
                request,
                response,
                HttpStatus.FORBIDDEN,
                "https://errors.societix.dev/forbidden",
                "Forbidden",
                "The authenticated account is not currently authorized",
                "SOC-403-FORBIDDEN");
    }
}
