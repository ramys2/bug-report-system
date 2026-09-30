package com.ramy.bugreport.dto.component;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * Response of the {@code PATCH /api/components/{componentId}/...} endpoints.
 *
 * @param id id of the component
 * @param message confirmation text
 */
@Schema(description = "Response of the `PATCH /api/components/{componentId}/...` endpoints.")
public record UpdateComponentResponse(
        @Schema(description = "Id of the component.", example = ApiExamples.UUID, requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,
        @Schema(description = "Confirmation text.", example = "Component updated successfully!", requiredMode = Schema.RequiredMode.REQUIRED)
        String message
) {
}
