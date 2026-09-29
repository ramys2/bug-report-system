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

/**
 * REST endpoints for user accounts, under {@code /api/accounts}.
 *
 * <p>Errors are returned as JSON {@code {"message": "..."}} ({@link com.ramy.bugreport.exception.ApiErrorResponse}):
 * 400 for invalid input, 401 when not signed in, 403 when the role or ownership check fails,
 * 404 when a referenced resource does not exist, 409 for business rule conflicts. Requests that
 * change data ({@code POST}, {@code PATCH}, {@code DELETE}) also need a CSRF token, see {@code GET /api/csrf}.
 */
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

    /**
     * {@code GET /api/accounts}: lists all accounts.
     *
     * <p>Access: ADMIN role.
     *
     * @return 200 with a list of {@code {id, username, email, role}}, where {@code username} is the account's display name
     */
    @GetMapping
    public List<UserAccountResponse> getAll() {
        return userAccountService.getAll();
    }

    /**
     * {@code GET /api/accounts/developers}: lists the users with the DEVELOPER role, e.g. to choose an assignee.
     *
     * <p>Access: any signed-in user.
     *
     * @return 200 with a list of {@code {id, name}}
     */
    @GetMapping("/developers")
    public List<DeveloperResponse> getDevelopers() {
        return userAccountService.getDevelopers();
    }

    /**
     * {@code GET /api/accounts/users?search=...}: finds users whose name contains the text, ignoring case.
     *
     * <p>Access: ADMIN or DEVELOPER role.
     *
     * @param search optional text to look for
     * @return 200 with a list of {@code {userId, name}}; empty if {@code search} is missing or blank
     */
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

    /**
     * {@code POST /api/accounts}: registers a new account with the REPORTER role.
     *
     * <p>Access: public, no sign-in needed.
     *
     * @param request body {@code {username, email, password}}, all required and not blank; {@code email} must be a valid address
     * @return 201 with {@code {id, message}}
     * @throws com.ramy.bugreport.exception.DuplicateEmailException 409 if the (trimmed, lower-cased) email is already registered
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
    
    /**
     * {@code PATCH /api/accounts/{userId}/role}: changes a user's role. Responds 204 with no body on success.
     *
     * <p>Access: ADMIN role, and not for the caller's own account.
     *
     * @param userId id of the account
     * @param request body {@code {role}}, required, one of REPORTER, DEVELOPER, ADMIN
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if the account does not exist
     * @throws com.ramy.bugreport.exception.BusinessRuleConflictException 409 if this would remove the last admin or would demote a developer who still has unclosed assigned reports
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
