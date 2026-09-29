package com.ramy.bugreport.dto.component;

import jakarta.validation.constraints.NotNull;

/**
 * Request body of {@code PATCH /api/components/{componentId}/description}.
 *
 * @param description the new description; required (may be empty)
 */
public record UpdateComponentDescriptionRequest(@NotNull String description) {
}
