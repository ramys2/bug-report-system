package com.ramy.bugreport.dto.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body of {@code POST /api/accounts} (registration).
 *
 * @param username display name; required, not blank
 * @param email email address; required, not blank, must be a valid address
 * @param password plain-text password; required, not blank (the service stores only its hash)
 */
public record CreateUserAccountRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
