package com.ramy.bugreport.dto.account;

import java.util.UUID;

import com.ramy.bugreport.domain.UserAccount;

public record DeveloperResponse(
        UUID id,
        String name
) {

    public static DeveloperResponse from(UserAccount userAccount) {
        return new DeveloperResponse(userAccount.getId(), userAccount.getName());
    }
}
