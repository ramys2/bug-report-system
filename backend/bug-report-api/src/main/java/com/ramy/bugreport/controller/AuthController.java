package com.ramy.bugreport.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ramy.bugreport.dto.account.UserAccountResponse;
import com.ramy.bugreport.security.UserAccountDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.ramy.bugreport.openapi.UnauthorizedResponse;

/**
 * Endpoints about the current session, under {@code /api/auth}.
 *
 * <p>Login ({@code POST /api/auth/login}, form fields) and logout ({@code POST /api/auth/logout}) are not implemented here
 * but configured in {@link com.ramy.bugreport.security.SecurityConfig}; both answer 200 on success and login answers 401 on failure.
 */
@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * {@code GET /api/auth/me}: returns the signed-in user.
     *
     * <p>Access: any signed-in user (401 otherwise).
     *
     * @return 200 with {@code {id, name, email, role}}
     */
    @Operation(summary = "Get the signed-in user", description = "Returns the account of the current session. Access: any signed-in user. Login (`POST /api/auth/login`) and logout (`POST /api/auth/logout`) are handled by Spring Security, not by this controller.")
    @ApiResponse(responseCode = "200", description = "The signed-in user.")
    @UnauthorizedResponse
    @GetMapping("/me")
    public UserAccountResponse me(
            @AuthenticationPrincipal UserAccountDetails authenticatedUser
    ) {
        return new UserAccountResponse(
                authenticatedUser.getId(),
                authenticatedUser.getName(),
                authenticatedUser.getUsername(),
                authenticatedUser.getRole()
        );
    }
}
