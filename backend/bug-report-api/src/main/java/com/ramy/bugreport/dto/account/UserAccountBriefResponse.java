package com.ramy.bugreport.dto.account;

import java.util.UUID;

import com.ramy.bugreport.domain.UserAccount;

/**
 * A search hit in {@code GET /api/accounts/users}.
 *
 * @param userId account id
 * @param name display name
 */
public record UserAccountBriefResponse(
        UUID userId,
        String name
) {

    public static UserAccountBriefResponse from(UserAccount userAccount) {
        return new UserAccountBriefResponse(userAccount.getId(), userAccount.getName());
    }
}
