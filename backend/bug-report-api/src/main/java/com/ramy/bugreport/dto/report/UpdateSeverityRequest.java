package com.ramy.bugreport.dto.report;

import com.ramy.bugreport.domain.EBugSeverity;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body of {@code PATCH /api/reports/{reportId}/severity}.
 *
 * @param severity the new severity; required
 */
@Schema(description = "Request body of `PATCH /api/reports/{reportId}/severity`.")
public record UpdateSeverityRequest(
        @Schema(description = "The new severity.", example = "CRITICAL")
        @NotNull EBugSeverity severity
) {
}
