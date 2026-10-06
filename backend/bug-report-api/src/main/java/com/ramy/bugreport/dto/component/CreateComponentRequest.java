package com.ramy.bugreport.dto.component;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * Request body of {@code POST /api/components}.
 *
 * @param name display name; required, not blank
 * @param projectId id of the project the component belongs to; required, the project must exist
 * @param description optional description
 * @param responsibleUserId id of the responsible user; optional; if given, the user must exist
 */
@Schema(description = "Request body of `POST /api/components`.")
public record CreateComponentRequest(
        @Schema(description = "Display name.", example = "Backend API")
        @NotBlank String name,
        @Schema(description = "Id of the project the component belongs to. Must belong to an existing project.", example = ApiExamples.UUID)
        @NotNull UUID projectId,
        @Schema(description = "Optional description.", example = "REST API and persistence layer.")
        String description,
        @Schema(description = "Optional id of the responsible user. Must belong to an existing user.", example = ApiExamples.UUID)
        UUID responsibleUserId
) {
}
