package com.ramy.bugreport.dto.report;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body of {@code POST /api/reports/{reportId}/resolution}.
 *
 * @param description what was done to fix the bug; required, not blank
 * @param fixedVersion version containing the fix; optional
 * @param commitUrl URL of the fixing commit; optional
 */
public record CloseBugReportRequest(
        @NotBlank String description,
        String fixedVersion,
        String commitUrl
) {
}
