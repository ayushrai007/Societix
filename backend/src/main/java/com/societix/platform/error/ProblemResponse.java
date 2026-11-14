package com.societix.platform.error;

import java.util.List;

public record ProblemResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String code,
        List<FieldError> errors) {

    public record FieldError(String field, String message) {}
}
