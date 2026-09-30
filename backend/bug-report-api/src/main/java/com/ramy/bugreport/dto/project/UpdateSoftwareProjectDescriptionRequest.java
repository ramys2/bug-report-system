package com.ramy.bugreport.dto.project;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body of {@code PATCH /api/projects/{projectId}/description}.
 *
 * @param description the new description; required (may be empty)
 */
@Schema(description = "Request body of `PATCH /api/projects/{projectId}/description`.")
public record UpdateSoftwareProjectDescriptionRequest(
        @Schema(description = "The new description.", example = "Application for reporting and resolving software defects.")
        @NotNull String description
) {
}
