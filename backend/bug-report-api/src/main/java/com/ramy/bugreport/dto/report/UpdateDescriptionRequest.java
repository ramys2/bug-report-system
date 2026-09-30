package com.ramy.bugreport.dto.report;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body of {@code PATCH /api/reports/{reportId}/description}.
 *
 * @param description the new description; required (may be empty)
 */
@Schema(description = "Request body of `PATCH /api/reports/{reportId}/description`.")
public record UpdateDescriptionRequest(
        @Schema(description = "The new description.", example = "The sign-in endpoint returns HTTP 500 for valid credentials.")
        @NotNull String description
) {
}
