package com.ramy.bugreport.dto.report;

import com.ramy.bugreport.domain.EBugStatus;

import jakarta.validation.constraints.NotNull;

/**
 * Request body of {@code PATCH /api/reports/{reportId}/status}.
 *
 * @param status the new status; required; {@code CLOSED} is rejected
 */
public record UpdateStatusRequest(@NotNull EBugStatus status) {
}
