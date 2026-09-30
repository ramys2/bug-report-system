package com.ramy.bugreport.dto.report;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * Request body of {@code PATCH /api/reports/{reportId}/assignee}.
 *
 * @param assigneeId id of the new assignee, who must be a developer; required
 */
@Schema(description = "Request body of `PATCH /api/reports/{reportId}/assignee`.")
public record UpdateAssigneeRequest(
        @Schema(description = "Id of the new assignee, who must be a developer.", example = ApiExamples.UUID)
        @NotNull UUID assigneeId
) {
}
