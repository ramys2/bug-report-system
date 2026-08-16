package com.ramy.bugreport.dto.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserAccountRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
