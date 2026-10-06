package com.ramy.bugreport.dto.report;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * Request body of {@code PATCH /api/reports/{reportId}/component}.
 *
 * @param componentId id of the new component, which must belong to the report's project; {@code null} removes the component
 */
@Schema(description = "Request body of `PATCH /api/reports/{reportId}/component`.")
public record UpdateComponentRequest(
        @Schema(description = "Id of the new component, which must belong to the report's project. Null removes the component.", example = ApiExamples.UUID)
        UUID componentId
) {
}
