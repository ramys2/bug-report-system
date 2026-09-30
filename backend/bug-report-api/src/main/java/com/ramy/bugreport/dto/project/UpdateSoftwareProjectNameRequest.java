package com.ramy.bugreport.dto.project;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body of {@code PATCH /api/projects/{projectId}/name}.
 *
 * @param name the new name; required, not blank
 */
@Schema(description = "Request body of `PATCH /api/projects/{projectId}/name`.")
public record UpdateSoftwareProjectNameRequest(
        @Schema(description = "The new name.", example = "Bug Report System")
        @NotBlank String name
) {
}
