package com.ramy.bugreport.dto.report;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body of {@code PATCH /api/reports/{reportId}/actual-behavior}.
 *
 * @param actualBehavior the new actual behavior; required (may be empty)
 */
@Schema(description = "Request body of `PATCH /api/reports/{reportId}/actual-behavior`.")
public record UpdateActualBehaviorRequest(
        @Schema(description = "The new actual behavior.", example = "The server returns HTTP 500 and the user remains signed out.")
        @NotNull String actualBehavior
) {
}
