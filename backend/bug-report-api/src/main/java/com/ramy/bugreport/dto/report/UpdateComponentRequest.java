package com.ramy.bugreport.dto.report;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Request body of {@code PATCH /api/reports/{reportId}/component}.
 *
 * @param componentId id of the new component; required
 */
public record UpdateComponentRequest(@NotNull UUID componentId) {
}
