package com.ramy.bugreport.dto.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body of {@code POST /api/accounts} (registration).
 *
 * @param name display name; required, not blank
 * @param email email address; required, not blank, must be a valid address
 * @param password plain-text password; required, not blank (the service stores only its hash)
 */
@Schema(description = "Request body of `POST /api/accounts` (registration).")
public record CreateUserAccountRequest(
        @Schema(description = "Display name.", example = "Alice Admin")
        @NotBlank String name,
        @Schema(description = "Email address.", example = "alice@example.com")
        @NotBlank @Email String email,
        @Schema(description = "Plain-text password.", example = "S3cret-pass!")
        @NotBlank String password
) {
}
