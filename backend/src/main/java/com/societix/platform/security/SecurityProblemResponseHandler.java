package com.societix.platform.security;

import com.societix.platform.error.ProblemResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Renders Spring Security failures in the project's standard problem response format. */
@Component
final class SecurityProblemResponseHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final BearerTokenAuthenticationEntryPoint bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();

    SecurityProblemResponseHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {
        bearerEntryPoint.commence(request, response, exception);
        writeProblem(
                request,
                response,
                401,
                "Unauthenticated",
                "A valid Firebase bearer token is required",
                "SOC-401-UNAUTHENTICATED");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        writeProblem(
                request,
                response,
                403,
                "Forbidden",
                "The authenticated account is not currently authorized",
                "SOC-403-FORBIDDEN");
    }

    private void writeProblem(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String title,
            String detail,
            String code)
            throws IOException {
        ProblemResponse body = new ProblemResponse(
                "https://errors.societix.dev/" + (status == 401 ? "unauthenticated" : "forbidden"),
                title,
                status,
                detail,
                request.getRequestURI(),
                code,
                List.of());
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
