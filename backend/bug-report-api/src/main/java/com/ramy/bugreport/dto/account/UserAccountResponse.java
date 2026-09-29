package com.ramy.bugreport.dto.account;

import java.util.UUID;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.UserAccount;

/**
 * A user account as returned by {@code GET /api/accounts} and {@code GET /api/auth/me}. Never contains the password hash.
 *
 * @param id account id
 * @param username the account's display name (despite the field name, not a login name)
 * @param email email address, which is also the login name
 * @param role the user's role
 */
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
