package com.ramy.bugreport.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ramy.bugreport.dto.account.UserAccountResponse;
import com.ramy.bugreport.security.UserAccountDetails;

/**
 * Endpoints about the current session, under {@code /api/auth}.
 *
 * <p>Login ({@code POST /api/auth/login}, form fields) and logout ({@code POST /api/auth/logout}) are not implemented here
 * but configured in {@link com.ramy.bugreport.security.SecurityConfig}; both answer 200 on success and login answers 401 on failure.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * {@code GET /api/auth/me}: returns the signed-in user.
     *
     * <p>Access: any signed-in user (401 otherwise).
     *
     * @return 200 with {@code {id, username, email, role}}, where {@code username} is the display name
     */
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
