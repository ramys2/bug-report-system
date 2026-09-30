package com.ramy.bugreport.dto.report;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * Response of {@code POST /api/reports/{reportId}/resolution}.
 *
 * @param reportId id of the closed report
 * @param message confirmation text
 */
@Schema(description = "Response of `POST /api/reports/{reportId}/resolution`.")
public record CloseBugReportResponse(
        @Schema(description = "Id of the closed report.", example = ApiExamples.UUID, requiredMode = Schema.RequiredMode.REQUIRED)
        UUID reportId,
        @Schema(description = "Confirmation text.", example = "Task has been closed successfully!", requiredMode = Schema.RequiredMode.REQUIRED)
        String message
) {

}
