package com.ramy.bugreport.dto.report;

import java.util.UUID;

/**
 * Response of {@code POST /api/reports}.
 *
 * @param id id of the new report
 * @param message confirmation text
 */
public record CreateBugReportResponse(UUID id, String message) {
}
