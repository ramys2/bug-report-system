package com.ramy.bugreport.dto.account;

import java.util.UUID;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.UserAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * A user account as returned by {@code GET /api/accounts} and {@code GET /api/auth/me}. Never contains the password hash.
 *
 * @param id account id
 * @param username the account's display name (despite the field name, not a login name)
 * @param email email address, which is also the login name
 * @param role the user's role
 */
@Schema(description = "A user account as returned by `GET /api/accounts` and `GET /api/auth/me`. Never contains the password hash.")
public record UserAccountResponse(
        @Schema(description = "Account id.", example = ApiExamples.UUID, requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,
        @Schema(description = "The account's display name (despite the field name, not a login name).", example = "Alice Admin", requiredMode = Schema.RequiredMode.REQUIRED)
        String username,
        @Schema(description = "Email address, which is also the login name.", example = "alice@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,
        @Schema(description = "The user's role.", example = "DEVELOPER", requiredMode = Schema.RequiredMode.REQUIRED)
        EUserRole role
) {

    public static UserAccountResponse from(UserAccount userAccount) {
        return new UserAccountResponse(
                userAccount.getId(),
                userAccount.getName(),
                userAccount.getEmailAddress(),
                userAccount.getRole());
    }
}
