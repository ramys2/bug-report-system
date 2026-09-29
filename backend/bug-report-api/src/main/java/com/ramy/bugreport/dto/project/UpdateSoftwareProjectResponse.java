package com.ramy.bugreport.dto.project;

import java.util.UUID;

/**
 * Response of the {@code PATCH /api/projects/{projectId}/...} endpoints.
 *
 * @param id id of the project
 * @param message confirmation text
 */
public record UpdateSoftwareProjectResponse(UUID id, String message) {
}
