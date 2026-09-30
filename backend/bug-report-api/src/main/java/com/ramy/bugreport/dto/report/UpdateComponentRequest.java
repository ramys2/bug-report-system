package com.ramy.bugreport.dto.report;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * Request body of {@code PATCH /api/reports/{reportId}/component}.
 *
 * @param componentId id of the new component; required
 */
@Schema(description = "Request body of `PATCH /api/reports/{reportId}/component`.")
public record UpdateComponentRequest(
        @Schema(description = "Id of the new component.", example = ApiExamples.UUID)
        @NotNull UUID componentId
) {
}
