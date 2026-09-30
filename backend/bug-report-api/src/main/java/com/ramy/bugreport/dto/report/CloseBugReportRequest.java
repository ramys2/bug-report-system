package com.ramy.bugreport.dto.report;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body of {@code POST /api/reports/{reportId}/resolution}.
 *
 * @param description what was done to fix the bug; required, not blank
 * @param fixedVersion version containing the fix; optional
 * @param commitUrl URL of the fixing commit; optional
 */
@Schema(description = "Request body of `POST /api/reports/{reportId}/resolution`.")
public record CloseBugReportRequest(
        @Schema(description = "What was done to fix the bug.", example = "Handle an absent optional avatar before constructing the user response.")
        @NotBlank String description,
        @Schema(description = "Version containing the fix.", example = "0.1.1")
        String fixedVersion,
        @Schema(description = "URL of the fixing commit.", example = "https://example.invalid/commits/7a21c9d")
        String commitUrl
) {
}
