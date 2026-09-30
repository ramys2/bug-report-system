package com.ramy.bugreport.dto.component;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body of {@code PATCH /api/components/{componentId}/name}.
 *
 * @param name the new name; required, not blank
 */
@Schema(description = "Request body of `PATCH /api/components/{componentId}/name`.")
public record UpdateComponentNameRequest(
        @Schema(description = "The new name.", example = "Backend API")
        @NotBlank String name
) {
}
