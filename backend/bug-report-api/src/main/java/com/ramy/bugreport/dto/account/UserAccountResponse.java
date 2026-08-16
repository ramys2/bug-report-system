package com.ramy.bugreport.dto.account;

import java.util.UUID;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.UserAccount;

public record UserAccountResponse(
        UUID id,
        String username,
        String email,
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
