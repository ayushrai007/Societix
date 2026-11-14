package com.societix.platform.error;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemResponse> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ProblemResponse.FieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(org.springframework.validation.FieldError::getField))
                .map(error -> new ProblemResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return problem(
                400,
                "Validation failed",
                "Request validation failed",
                "SOC-400-VALIDATION",
                request,
                errors);
    }

    @ExceptionHandler(UnauthenticatedException.class)
    ResponseEntity<ProblemResponse> unauthenticated(HttpServletRequest request) {
        return problem(401, "Unauthenticated", "Admin authentication is required", "SOC-401-UNAUTHENTICATED", request, List.of());
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<ProblemResponse> forbidden(HttpServletRequest request) {
        return problem(403, "Forbidden", "Platform admin permission is required", "SOC-403-FORBIDDEN", request, List.of());
    }

    private ResponseEntity<ProblemResponse> problem(
            int status,
            String title,
            String detail,
            String code,
            HttpServletRequest request,
            List<ProblemResponse.FieldError> errors) {
        ProblemResponse body = new ProblemResponse(
                errorType(code),
                title,
                status,
                detail,
                request.getRequestURI(),
                code,
                errors);
        return ResponseEntity.status(status)
                .contentType(MediaType.valueOf("application/problem+json"))
                .body(body);
    }

    private String errorType(String code) {
        return switch (code) {
            case "SOC-400-VALIDATION" -> "https://errors.societix.dev/validation";
            case "SOC-401-UNAUTHENTICATED" -> "https://errors.societix.dev/unauthenticated";
            case "SOC-403-FORBIDDEN" -> "https://errors.societix.dev/forbidden";
            default -> "https://errors.societix.dev/internal";
        };
    }
}
