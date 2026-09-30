package com.ramy.bugreport.dto.account;

import java.util.UUID;

import com.ramy.bugreport.domain.UserAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * A developer in {@code GET /api/accounts/developers}.
 *
 * @param id account id
 * @param name display name
 */
@Schema(description = "A developer in `GET /api/accounts/developers`.")
public record DeveloperResponse(
        @Schema(description = "Account id.", example = ApiExamples.UUID, requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,
        @Schema(description = "Display name.", example = "Daniel Developer", requiredMode = Schema.RequiredMode.REQUIRED)
        String name
) {

    public static DeveloperResponse from(UserAccount userAccount) {
        return new DeveloperResponse(userAccount.getId(), userAccount.getName());
    }
}
