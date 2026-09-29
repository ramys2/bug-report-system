package com.ramy.bugreport.dto.project;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body of {@code POST /api/projects}.
 *
 * @param name display name; required, not blank
 * @param description optional description
 */
public record CreateSoftwareProjectRequest(
        @NotBlank String name,
        String description
) {
}
