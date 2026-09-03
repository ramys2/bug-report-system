package com.ramy.bugreport.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.dto.account.CreateUserAccountRequest;
import com.ramy.bugreport.dto.account.CreateUserAccountResponse;
import com.ramy.bugreport.dto.account.DeveloperResponse;
import com.ramy.bugreport.dto.account.UserAccountResponse;
import com.ramy.bugreport.exception.AdminAccountDeletionException;
import com.ramy.bugreport.exception.ResourceNotFoundException;
import com.ramy.bugreport.repository.IUserAccountRepository;

import jakarta.transaction.Transactional;

@Service
public class UserAccountService {
    private final IUserAccountRepository userAccountRepository;

    public UserAccountService(IUserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
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

    /*
    * ============================================
    *
    * POST
    *
    * ============================================
    */

    @Transactional
    public CreateUserAccountResponse create(CreateUserAccountRequest request) {
        UserAccount userAccount = new UserAccount(
                request.username(),
                request.email(),
                request.password(),
                EUserRole.REPORTER);
        userAccount = userAccountRepository.save(userAccount);

        return new CreateUserAccountResponse(userAccount.getId(), "Successfully created!");
    }
    
    
    
    /*
    * ============================================
    *
    * DELETE
    *
    * ============================================
    */
    
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(UUID userId) {
    	UserAccount account = userAccountRepository.findById(userId)
    		.orElseThrow(() -> new ResourceNotFoundException("User with id %s doesn't exist!".formatted(userId)));
    	
    	if (account.getRole() == EUserRole.ADMIN) {
    		throw new AdminAccountDeletionException("Admin accounts must be assigned a different role before deletion.");
    	}
    	
    	userAccountRepository.delete(account);
    }
}
