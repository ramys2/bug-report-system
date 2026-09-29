package com.ramy.bugreport.dto.account;

import java.util.UUID;

import com.ramy.bugreport.domain.UserAccount;

/**
 * A developer in {@code GET /api/accounts/developers}.
 *
 * @param id account id
 * @param name display name
 */
public record DeveloperResponse(
        UUID id,
        String name
) {

    public static DeveloperResponse from(UserAccount userAccount) {
        return new DeveloperResponse(userAccount.getId(), userAccount.getName());
    }
}
