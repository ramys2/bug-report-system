package com.ramy.bugreport.dto.account;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body of {@code POST /api/accounts} (registration).
 *
 * @param name unique username, also used as the display name; required, not blank, up to 255 characters
 * @param email email address; required, not blank, must be a valid address
 * @param password plain-text password; required, not blank (the service stores only its hash)
 */
@Schema(description = "Request body of `POST /api/accounts` (registration).")
public record CreateUserAccountRequest(
        @Schema(description = "Unique username, also used as the display name.", example = "Alice Admin")
        @NotBlank @Size(max = 255) String name,
        @Schema(description = "Email address.", example = "alice@example.com")
        @NotBlank @Email @Size(max = 255) String email,
        @Schema(description = "Plain-text password.", example = "S3cret-pass!")
        @NotBlank String password
) {
    /** BCrypt accepts at most 72 UTF-8 bytes; character counts alone miss multi-byte characters. */
    @JsonIgnore
    @AssertTrue(message = "Password must not exceed 72 UTF-8 bytes.")
    public boolean isPasswordLengthValid() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
