package com.ramy.bugreport.dto.component;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body of {@code PATCH /api/components/{componentId}/name}.
 *
 * @param name the new name; required, not blank
 */
public record UpdateComponentNameRequest(@NotBlank String name) {
}
