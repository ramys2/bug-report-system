package com.ramy.bugreport.dto.project;

import java.util.UUID;

import com.ramy.bugreport.domain.SoftwareProject;

/**
 * A project in {@code GET /api/projects}.
 *
 * @param id project id
 * @param name display name
 * @param description description; may be null
 */
public record SoftwareProjectResponse(
        UUID id,
        String name,
        String description
) {

    public static SoftwareProjectResponse from(SoftwareProject project) {
        return new SoftwareProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription());
    }
}
