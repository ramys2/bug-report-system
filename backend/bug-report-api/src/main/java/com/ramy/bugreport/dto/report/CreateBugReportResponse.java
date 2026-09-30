package com.ramy.bugreport.dto.report;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * Response of {@code POST /api/reports}.
 *
 * @param id id of the new report
 * @param message confirmation text
 */
@Schema(description = "Response of `POST /api/reports`.")
public record CreateBugReportResponse(
        @Schema(description = "Id of the new report.", example = ApiExamples.UUID, requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,
        @Schema(description = "Confirmation text.", example = "Successfully created!", requiredMode = Schema.RequiredMode.REQUIRED)
        String message
) {
}
