package com.societix.platform.society.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSocietyRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 1000) String address,
        @NotBlank @Pattern(regexp = "[A-Z]{2}") String state) {}
