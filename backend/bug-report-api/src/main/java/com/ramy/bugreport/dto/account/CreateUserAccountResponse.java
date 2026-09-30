package com.ramy.bugreport.dto.account;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * Response of {@code POST /api/accounts}.
 *
 * @param id id of the new account
 * @param message confirmation text
 */
@Schema(description = "Response of `POST /api/accounts`.")
public record CreateUserAccountResponse(
        @Schema(description = "Id of the new account.", example = ApiExamples.UUID, requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,
        @Schema(description = "Confirmation text.", example = "Successfully created!", requiredMode = Schema.RequiredMode.REQUIRED)
        String message
) {
}
