package com.ramy.bugreport.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ramy.bugreport.dto.account.CreateUserAccountRequest;
import com.ramy.bugreport.dto.account.CreateUserAccountResponse;
import com.ramy.bugreport.dto.account.DeveloperResponse;
import com.ramy.bugreport.dto.account.UpdateRoleRequest;
import com.ramy.bugreport.dto.account.UserAccountResponse;
import com.ramy.bugreport.dto.account.UserAccountBriefResponse;
import com.ramy.bugreport.service.UserAccountService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts")
public class UserAccountController {
    private final UserAccountService userAccountService;

    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    /*
    * ============================================
    *
    * GET Mappings
    *
    * ============================================
    */

    @GetMapping
    public List<UserAccountResponse> getAll() {
        return userAccountService.getAll();
    }

    @GetMapping("/developers")
    public List<DeveloperResponse> getDevelopers() {
        return userAccountService.getDevelopers();
    }

    @GetMapping("/users")
    public List<UserAccountBriefResponse> searchUsers(@RequestParam(required = false) String search) {
        return userAccountService.searchUsers(search);
    }

    /*
    * ============================================
    *
    * POST Mappings
    *
    * ============================================
    */

    @PostMapping
    public ResponseEntity<CreateUserAccountResponse> create(
            @Valid @RequestBody CreateUserAccountRequest request
    ) {
        var response = userAccountService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    
    /*
     * ============================================
     *
     * PATCH Mappings
     *
     * ============================================
     */
    
    @PatchMapping("/{userId}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        userAccountService.updateRole(userId, request);
    }
}
