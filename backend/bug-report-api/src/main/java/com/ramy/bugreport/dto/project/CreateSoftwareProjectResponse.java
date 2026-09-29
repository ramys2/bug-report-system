package com.ramy.bugreport.dto.project;

import java.util.UUID;

/**
 * Response of {@code POST /api/projects}.
 *
 * @param id id of the new project
 * @param message confirmation text
 */
public record CreateSoftwareProjectResponse(UUID id, String message) {
}
