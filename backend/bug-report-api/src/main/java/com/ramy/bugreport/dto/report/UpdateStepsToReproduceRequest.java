package com.ramy.bugreport.dto.report;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body of {@code PATCH /api/reports/{reportId}/steps-to-reproduce}.
 *
 * @param stepsToReproduce the new steps to reproduce; required (may be empty)
 */
@Schema(description = "Request body of `PATCH /api/reports/{reportId}/steps-to-reproduce`.")
public record UpdateStepsToReproduceRequest(
        @Schema(description = "The new steps to reproduce.", example = "Open sign in, enter valid credentials, and submit the form.")
        @NotNull String stepsToReproduce
) {
}
