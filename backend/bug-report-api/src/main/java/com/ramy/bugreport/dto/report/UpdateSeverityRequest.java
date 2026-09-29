package com.ramy.bugreport.dto.report;

import com.ramy.bugreport.domain.EBugSeverity;

import jakarta.validation.constraints.NotNull;

/**
 * Request body of {@code PATCH /api/reports/{reportId}/severity}.
 *
 * @param severity the new severity; required
 */
public record UpdateSeverityRequest(@NotNull EBugSeverity severity) {
}
