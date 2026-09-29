package com.ramy.bugreport.dto.account;

import com.ramy.bugreport.domain.EUserRole;

import jakarta.validation.constraints.NotNull;

/**
 * Request body of {@code PATCH /api/accounts/{userId}/role}.
 *
 * @param role the new role; required
 */
public record UpdateRoleRequest(@NotNull EUserRole role) {

}
