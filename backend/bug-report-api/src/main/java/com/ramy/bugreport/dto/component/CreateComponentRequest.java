package com.ramy.bugreport.dto.component;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body of {@code POST /api/components}.
 *
 * @param name display name; required, not blank
 * @param description optional description
 * @param responsibleUserId id of the responsible user; not validated as required, but the database requires one
 */
public record CreateComponentRequest(
        @NotBlank String name,
        String description,
        UUID responsibleUserId
) {
}
