package com.ramy.bugreport.dto.account;

import java.util.UUID;

import com.ramy.bugreport.domain.UserAccount;

public record UserAccountBriefResponse(
        UUID userId,
        String name
) {

    public static UserAccountBriefResponse from(UserAccount userAccount) {
        return new UserAccountBriefResponse(userAccount.getId(), userAccount.getName());
    }
}
