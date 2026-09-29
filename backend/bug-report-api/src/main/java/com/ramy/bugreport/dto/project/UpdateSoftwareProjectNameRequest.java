package com.ramy.bugreport.dto.project;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body of {@code PATCH /api/projects/{projectId}/name}.
 *
 * @param name the new name; required, not blank
 */
public record UpdateSoftwareProjectNameRequest(@NotBlank String name) {
}
