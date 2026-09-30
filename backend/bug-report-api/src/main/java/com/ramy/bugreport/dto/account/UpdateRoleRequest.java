package com.ramy.bugreport.dto.account;

import com.ramy.bugreport.domain.EUserRole;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body of {@code PATCH /api/accounts/{userId}/role}.
 *
 * @param role the new role; required
 */
@Schema(description = "Request body of `PATCH /api/accounts/{userId}/role`.")
public record UpdateRoleRequest(
        @Schema(description = "The new role.", example = "DEVELOPER")
        @NotNull EUserRole role
) {

}
