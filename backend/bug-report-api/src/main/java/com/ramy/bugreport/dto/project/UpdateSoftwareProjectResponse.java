package com.ramy.bugreport.dto.project;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * Response of the {@code PATCH /api/projects/{projectId}/...} endpoints.
 *
 * @param id id of the project
 * @param message confirmation text
 */
@Schema(description = "Response of the `PATCH /api/projects/{projectId}/...` endpoints.")
public record UpdateSoftwareProjectResponse(
        @Schema(description = "Id of the project.", example = ApiExamples.UUID, requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,
        @Schema(description = "Confirmation text.", example = "Project updated successfully!", requiredMode = Schema.RequiredMode.REQUIRED)
        String message
) {
}
