package com.ramy.bugreport.dto.report;

import com.ramy.bugreport.domain.EBugStatus;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body of {@code PATCH /api/reports/{reportId}/status}.
 *
 * @param status the new status; required; {@code ASSIGNED} and {@code CLOSED} are rejected
 */
@Schema(description = "Request body of `PATCH /api/reports/{reportId}/status`.")
public record UpdateStatusRequest(
        @Schema(description = "The new status.", example = "IN_PROGRESS")
        @NotNull EBugStatus status
) {
}
