package com.ramy.bugreport.dto.report;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * Response of {@code POST /api/reports/{reportId}/resolution}.
 *
 * @param reportId despite its name, the id of the newly created resolution, not of the report
 * @param message confirmation text
 */
@Schema(description = "Response of `POST /api/reports/{reportId}/resolution`.")
public record CloseBugReportResponse(
        @Schema(description = "Despite its name, the id of the newly created resolution, not of the report.", example = ApiExamples.UUID, requiredMode = Schema.RequiredMode.REQUIRED)
        UUID reportId,
        @Schema(description = "Confirmation text.", example = "Task has been closed successfully!", requiredMode = Schema.RequiredMode.REQUIRED)
        String message
) {

}
