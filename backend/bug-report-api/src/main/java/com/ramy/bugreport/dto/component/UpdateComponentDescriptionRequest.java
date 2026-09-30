package com.ramy.bugreport.dto.component;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body of {@code PATCH /api/components/{componentId}/description}.
 *
 * @param description the new description; required (may be empty)
 */
@Schema(description = "Request body of `PATCH /api/components/{componentId}/description`.")
public record UpdateComponentDescriptionRequest(
        @Schema(description = "The new description.", example = "REST API and persistence layer.")
        @NotNull String description
) {
}
