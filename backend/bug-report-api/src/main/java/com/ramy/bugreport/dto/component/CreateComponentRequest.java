package com.ramy.bugreport.dto.component;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * Request body of {@code POST /api/components}.
 *
 * @param name display name; required, not blank
 * @param description optional description
 * @param responsibleUserId id of the responsible user; not validated as required, but the database requires one
 */
@Schema(description = "Request body of `POST /api/components`.")
public record CreateComponentRequest(
        @Schema(description = "Display name.", example = "Backend API")
        @NotBlank String name,
        @Schema(description = "Optional description.", example = "REST API and persistence layer.")
        String description,
        @Schema(description = "Id of the responsible user.", example = ApiExamples.UUID)
        UUID responsibleUserId
) {
}
