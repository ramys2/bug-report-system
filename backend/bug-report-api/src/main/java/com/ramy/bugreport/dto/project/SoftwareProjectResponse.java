package com.ramy.bugreport.dto.project;

import java.util.UUID;

import com.ramy.bugreport.domain.SoftwareProject;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * A project in {@code GET /api/projects}.
 *
 * @param id project id
 * @param name display name
 * @param description description; may be null
 */
@Schema(description = "A project in `GET /api/projects`.")
public record SoftwareProjectResponse(
        @Schema(description = "Project id.", example = ApiExamples.UUID, requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,
        @Schema(description = "Display name.", example = "Bug Report System", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "Description. May be null.", example = "Application for reporting and resolving software defects.")
        String description
) {

    public static SoftwareProjectResponse from(SoftwareProject project) {
        return new SoftwareProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription());
    }
}
