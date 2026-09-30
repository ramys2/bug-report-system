package com.ramy.bugreport.dto.component;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * Response of {@code POST /api/components}.
 *
 * @param id id of the new component
 * @param message confirmation text
 */
@Schema(description = "Response of `POST /api/components`.")
public record CreateComponentResponse(
        @Schema(description = "Id of the new component.", example = ApiExamples.UUID, requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,
        @Schema(description = "Confirmation text.", example = "Successfully created!", requiredMode = Schema.RequiredMode.REQUIRED)
        String message
) {
}
