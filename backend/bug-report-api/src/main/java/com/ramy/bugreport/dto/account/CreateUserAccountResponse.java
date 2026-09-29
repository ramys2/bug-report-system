package com.ramy.bugreport.dto.account;

import java.util.UUID;

/**
 * Response of {@code POST /api/accounts}.
 *
 * @param id id of the new account
 * @param message confirmation text
 */
public record CreateUserAccountResponse(UUID id, String message) {
}
