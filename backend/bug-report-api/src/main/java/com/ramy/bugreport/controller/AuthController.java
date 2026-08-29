package com.ramy.bugreport.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ramy.bugreport.dto.account.UserAccountResponse;
import com.ramy.bugreport.security.UserAccountDetails;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

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
