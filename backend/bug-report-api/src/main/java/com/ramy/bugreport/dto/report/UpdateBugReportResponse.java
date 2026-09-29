package com.ramy.bugreport.dto.report;

import java.util.UUID;

/**
 * Response of the {@code PATCH /api/reports/{reportId}/...} endpoints.
 *
 * @param id id of the report
 * @param message confirmation text
 */
public record UpdateBugReportResponse(UUID id, String message) {

}
