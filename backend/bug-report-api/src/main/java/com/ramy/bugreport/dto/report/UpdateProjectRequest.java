package com.ramy.bugreport.dto.report;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * Request body of {@code PATCH /api/reports/{reportId}/project}.
 *
 * @param projectId id of the new project; required
 */
@Schema(description = "Request body of `PATCH /api/reports/{reportId}/project`.")
public record UpdateProjectRequest(
        @Schema(description = "Id of the new project.", example = ApiExamples.UUID)
        @NotNull UUID projectId
) {
}
