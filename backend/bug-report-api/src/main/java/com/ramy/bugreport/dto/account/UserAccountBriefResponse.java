package com.ramy.bugreport.dto.account;

import java.util.UUID;

import com.ramy.bugreport.domain.UserAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * A search hit in {@code GET /api/accounts/users}.
 *
 * @param id account id
 * @param name display name
 */
@Schema(description = "A search hit in `GET /api/accounts/users`.")
public record UserAccountBriefResponse(
        @Schema(description = "Account id.", example = ApiExamples.UUID, requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,
        @Schema(description = "Display name.", example = "Daniel Developer", requiredMode = Schema.RequiredMode.REQUIRED)
        String name
) {

    public static UserAccountBriefResponse from(UserAccount userAccount) {
        return new UserAccountBriefResponse(userAccount.getId(), userAccount.getName());
    }
}
