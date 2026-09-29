package com.ramy.bugreport.dto.report;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Request body of {@code PATCH /api/reports/{reportId}/project}.
 *
 * @param projectId id of the new project; required
 */
public record UpdateProjectRequest(@NotNull UUID projectId) {
}
