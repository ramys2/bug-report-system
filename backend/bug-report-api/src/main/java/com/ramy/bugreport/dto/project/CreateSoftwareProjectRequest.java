package com.ramy.bugreport.dto.project;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body of {@code POST /api/projects}.
 *
 * @param name display name; required, not blank
 * @param description optional description
 */
@Schema(description = "Request body of `POST /api/projects`.")
public record CreateSoftwareProjectRequest(
        @Schema(description = "Display name.", example = "Bug Report System")
        @NotBlank String name,
        @Schema(description = "Optional description.", example = "Application for reporting and resolving software defects.")
        String description
) {
}
