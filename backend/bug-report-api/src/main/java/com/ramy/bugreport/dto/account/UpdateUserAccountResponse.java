package com.ramy.bugreport.dto.account;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * Response of the {@code PATCH /api/accounts/{userId}/...} endpoints.
 *
 * @param id id of the account
 * @param message confirmation text
 */
@Schema(description = "Response of the `PATCH /api/accounts/{userId}/...` endpoints.")
public record UpdateUserAccountResponse(
        @Schema(description = "Id of the account.", example = ApiExamples.UUID, requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,
        @Schema(description = "Confirmation text.", example = "User role updated successfully!", requiredMode = Schema.RequiredMode.REQUIRED)
        String message
) {
}
