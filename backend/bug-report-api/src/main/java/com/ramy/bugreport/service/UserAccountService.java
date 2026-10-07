package com.ramy.bugreport.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.dto.PageResponse;
import com.ramy.bugreport.dto.account.CreateUserAccountRequest;
import com.ramy.bugreport.dto.account.CreateUserAccountResponse;
import com.ramy.bugreport.dto.account.DeveloperResponse;
import com.ramy.bugreport.dto.account.UpdateRoleRequest;
import com.ramy.bugreport.dto.account.UpdateUserAccountResponse;
import com.ramy.bugreport.dto.account.UserAccountResponse;
import com.ramy.bugreport.dto.account.UserAccountBriefResponse;
import com.ramy.bugreport.domain.EBugStatus;
import com.ramy.bugreport.exception.BusinessRuleConflictException;
import com.ramy.bugreport.exception.DuplicateEmailException;
import com.ramy.bugreport.exception.ResourceNotFoundException;
import com.ramy.bugreport.repository.IBugReportRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;
import com.ramy.bugreport.repository.PageQuery;
import com.ramy.bugreport.repository.PageResult;
import com.ramy.bugreport.repository.UserAccountFilter;

import jakarta.transaction.Transactional;

/** Business logic for user accounts: listing, searching, registration and role changes. */
@Service
public class UserAccountService {
    /** Largest page size a client can get from {@link #getAll}. */
    static final int MAX_PAGE_SIZE = 100;

    private final IUserAccountRepository userAccountRepository;
    private final IBugReportRepository bugReportRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(
            IUserAccountRepository userAccountRepository,
            IBugReportRepository bugReportRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userAccountRepository = userAccountRepository;
        this.bugReportRepository = bugReportRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /*
    * ============================================
    *
    * GET
    *
    * ============================================
    */

    /**
     * Returns one page of the accounts that match the filter, ordered by name. Requires the ADMIN role.
     *
     * <p>Text conditions are trimmed and ignored when blank. A negative page is treated as 0 and the size is
     * limited to 1..{@value #MAX_PAGE_SIZE}, so a client cannot request an unbounded amount of data.
     *
     * @param filter conditions for id, name, email and role; text conditions match parts of the value, ignoring case
     * @param page zero-based page number
     * @param size number of accounts per page
     */
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserAccountResponse> getAll(UserAccountFilter filter, int page, int size) {
        var normalizedFilter = new UserAccountFilter(
                blankToNull(filter.id()),
                blankToNull(filter.name()),
                blankToNull(filter.email()),
                filter.role());
        var pageQuery = new PageQuery(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));

        PageResult<UserAccount> result = userAccountRepository.findPage(normalizedFilter, pageQuery);

        return PageResponse.of(
                result.items().stream().map(UserAccountResponse::from).toList(),
                pageQuery.page(),
                pageQuery.size(),
                result.totalElements());
    }

    /** Returns the accounts with the {@code DEVELOPER} role. */
    public List<DeveloperResponse> getDevelopers() {
        return userAccountRepository.findByRole(EUserRole.DEVELOPER)
                .stream()
                .map(DeveloperResponse::from)
                .toList();
    }

    /**
     * Finds accounts whose name contains the search text, ignoring case.
     *
     * <p>Requires the ADMIN or DEVELOPER role.
     *
     * @param search text to look for; surrounding whitespace is trimmed
     * @return the matches, or an empty list if {@code search} is {@code null} or blank
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    public List<UserAccountBriefResponse> searchUsers(String search) {
        if (search == null || search.isBlank()) {
            return List.of();
        }

        return userAccountRepository.findByNameContainingIgnoreCase(search.trim())
                .stream()
                .map(UserAccountBriefResponse::from)
                .toList();
    }

    /*
    * ============================================
    *
    * POST
    *
    * ============================================
    */

    /**
     * Registers a new account with the {@code REPORTER} role. The email is trimmed and lower-cased and the password is stored as a hash produced by the configured {@code PasswordEncoder}.
     *
     * @return the id of the new account
     * @throws DuplicateEmailException if an account with the normalized email already exists
     */
    @Transactional
    public CreateUserAccountResponse create(CreateUserAccountRequest request) {
        String emailAddress = normalizeEmail(request.email());
        if (userAccountRepository.existsByEmailAddress(emailAddress)) {
            throw new DuplicateEmailException();
        }

        UserAccount userAccount = new UserAccount(
                request.name(),
                emailAddress,
                passwordEncoder.encode(request.password()),
                EUserRole.REPORTER);
        userAccount = userAccountRepository.save(userAccount);

        return new CreateUserAccountResponse(userAccount.getId(), "Successfully created!");
    }
    
    
    /*
     * ============================================
     *
     * PATCH
     *
     * ============================================
     */
    
    /**
     * Changes an account's role.
     *
     * <p>Requires the ADMIN role, and admins cannot change their own role
     * (checked by {@code UserAccountAuthorizer.canUpdateRole}).
     *
     * @param userId id of the account to change
     * @param request the new role
     * @throws ResourceNotFoundException if the account does not exist
     * @throws BusinessRuleConflictException if this would demote the last admin, or would take the
     *         {@code DEVELOPER} role from a user who still has unclosed assigned reports
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN') and @userAccountAuthorizer.canUpdateRole(#userId, authentication)")
    public UpdateUserAccountResponse updateRole(UUID userId, UpdateRoleRequest request) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id=%s does not exist!".formatted(userId)));

        if (account.getRole() == EUserRole.ADMIN && request.role() != EUserRole.ADMIN) {
            // Lock every admin row so concurrent demotions are serialized.
            if (userAccountRepository.findAllByRole(EUserRole.ADMIN).size() == 1) {
                throw new BusinessRuleConflictException("At least one admin account must remain.");
            }
        }

        if (account.getRole() == EUserRole.DEVELOPER && request.role() != EUserRole.DEVELOPER
                && bugReportRepository.existsByAssigneeIdAndStatusNot(userId, EBugStatus.CLOSED)) {
            throw new BusinessRuleConflictException(
                    "A developer with open bug report assignments cannot be assigned a different role.");
        }

        account.setRole(request.role());
        userAccountRepository.save(account);
        return new UpdateUserAccountResponse(userId, "User role updated successfully!");
    }

    /** Trims the text; returns {@code null} if it is {@code null} or blank, meaning "no condition". */
    private String blankToNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }

    /** Trims the email and lower-cases it, so that lookups are not case-sensitive. */
    private String normalizeEmail(String emailAddress) {
        return emailAddress.trim().toLowerCase(Locale.ROOT);
    }
}
