package com.ramy.bugreport.dto.report;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Request body of {@code PATCH /api/reports/{reportId}/assignee}.
 *
 * @param assigneeId id of the new assignee, who must be a developer; required
 */
public record UpdateAssigneeRequest(@NotNull UUID assigneeId) {
}
