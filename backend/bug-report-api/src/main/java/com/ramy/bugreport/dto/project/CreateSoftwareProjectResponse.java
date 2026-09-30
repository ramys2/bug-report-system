package com.ramy.bugreport.dto.project;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * Response of {@code POST /api/projects}.
 *
 * @param id id of the new project
 * @param message confirmation text
 */
@Schema(description = "Response of `POST /api/projects`.")
public record CreateSoftwareProjectResponse(
        @Schema(description = "Id of the new project.", example = ApiExamples.UUID, requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,
        @Schema(description = "Confirmation text.", example = "Successfully created!", requiredMode = Schema.RequiredMode.REQUIRED)
        String message
) {
}
