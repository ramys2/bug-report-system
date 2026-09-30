package com.ramy.bugreport.dto.report;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body of {@code PATCH /api/reports/{reportId}/expected-behavior}.
 *
 * @param expectedBehavior the new expected behavior; required (may be empty)
 */
@Schema(description = "Request body of `PATCH /api/reports/{reportId}/expected-behavior`.")
public record UpdateExpectedBehaviorRequest(
        @Schema(description = "The new expected behavior.", example = "The user is authenticated and redirected to the dashboard.")
        @NotNull String expectedBehavior
) {
}
