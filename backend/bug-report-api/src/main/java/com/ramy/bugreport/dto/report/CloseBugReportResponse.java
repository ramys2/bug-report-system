package com.ramy.bugreport.dto.report;

import java.util.UUID;

/**
 * Response of {@code POST /api/reports/{reportId}/resolution}.
 *
 * @param reportId despite its name, the id of the newly created resolution, not of the report
 * @param message confirmation text
 */
public record CloseBugReportResponse(UUID reportId, String message) {

}
