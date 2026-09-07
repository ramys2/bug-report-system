package com.ramy.bugreport.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.dto.account.CreateUserAccountRequest;
import com.ramy.bugreport.dto.account.CreateUserAccountResponse;
import com.ramy.bugreport.dto.account.DeveloperResponse;
import com.ramy.bugreport.dto.account.UpdateRoleRequest;
import com.ramy.bugreport.dto.account.UserAccountResponse;
import com.ramy.bugreport.dto.account.UserAccountBriefResponse;
import com.ramy.bugreport.domain.EBugStatus;
import com.ramy.bugreport.exception.BusinessRuleConflictException;
import com.ramy.bugreport.exception.DuplicateEmailException;
import com.ramy.bugreport.exception.ResourceNotFoundException;
import com.ramy.bugreport.repository.IBugReportRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;

import jakarta.transaction.Transactional;

@Service
public class UserAccountService {
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

    public List<UserAccountResponse> getAll() {
        return userAccountRepository.findAll()
                .stream()
                .map(UserAccountResponse::from)
                .toList();
    }

    public List<DeveloperResponse> getDevelopers() {
        return userAccountRepository.findAll()
                .stream()
                .filter(userAccount -> userAccount.getRole() == EUserRole.DEVELOPER)
                .map(DeveloperResponse::from)
                .toList();
    }

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

    @Transactional
    public CreateUserAccountResponse create(CreateUserAccountRequest request) {
        String emailAddress = normalizeEmail(request.email());
        if (userAccountRepository.existsByEmailAddress(emailAddress)) {
            throw new DuplicateEmailException();
        }

        UserAccount userAccount = new UserAccount(
                request.username(),
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
    
    @Transactional
    @PreAuthorize("hasRole('ADMIN') and @userAccountAuthorizer.canUpdateRole(#userId, authentication)")
    public void updateRole(UUID userId, UpdateRoleRequest request) {
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
    }

    private String normalizeEmail(String emailAddress) {
        return emailAddress.trim().toLowerCase(Locale.ROOT);
    }
}
