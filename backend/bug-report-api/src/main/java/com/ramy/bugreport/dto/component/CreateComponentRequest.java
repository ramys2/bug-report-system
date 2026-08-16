package com.ramy.bugreport.dto.component;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record CreateComponentRequest(
        @NotBlank String name,
        String description,
        UUID responsibleDeveloperId
) {
}
