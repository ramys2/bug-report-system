package com.ramy.bugreport.dto.component;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * Request body of {@code PATCH /api/components/{componentId}/responsibleUserId}.
 *
 * @param responsibleUserId id of the new responsible user; required
 */
@Schema(description = "Request body of `PATCH /api/components/{componentId}/responsibleUserId`.")
public record UpdateComponentResponsibleUserRequest(
        @Schema(description = "Id of the new responsible user.", example = ApiExamples.UUID)
        @NotNull UUID responsibleUserId
) {
}
