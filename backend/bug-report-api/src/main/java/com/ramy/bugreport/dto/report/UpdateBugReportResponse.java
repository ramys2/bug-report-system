package com.ramy.bugreport.dto.report;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * Response of the {@code PATCH /api/reports/{reportId}/...} endpoints.
 *
 * @param id id of the report
 * @param message confirmation text
 */
@Schema(description = "Response of the `PATCH /api/reports/{reportId}/...` endpoints.")
public record UpdateBugReportResponse(
        @Schema(description = "Id of the report.", example = ApiExamples.UUID, requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,
        @Schema(description = "Confirmation text.", example = "Bug report updated successfully!", requiredMode = Schema.RequiredMode.REQUIRED)
        String message
) {

}
