package com.ramy.bugreport.dto.project;

import jakarta.validation.constraints.NotNull;

/**
 * Request body of {@code PATCH /api/projects/{projectId}/description}.
 *
 * @param description the new description; required (may be empty)
 */
public record UpdateSoftwareProjectDescriptionRequest(@NotNull String description) {
}
