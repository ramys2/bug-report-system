package com.ramy.bugreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ramy.bugreport.domain.EBugStatus;
import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.dto.account.CreateUserAccountRequest;
import com.ramy.bugreport.dto.account.DeveloperResponse;
import com.ramy.bugreport.dto.account.UpdateRoleRequest;
import com.ramy.bugreport.dto.account.UserAccountResponse;
import com.ramy.bugreport.dto.account.UserAccountBriefResponse;
import com.ramy.bugreport.exception.BusinessRuleConflictException;
import com.ramy.bugreport.exception.DuplicateEmailException;
import com.ramy.bugreport.repository.IBugReportRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock private IUserAccountRepository userAccountRepository;
    @Mock private IBugReportRepository bugReportRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private UserAccountService service;

    @BeforeEach
    void setUp() {
        service = new UserAccountService(userAccountRepository, bugReportRepository, passwordEncoder);
    }

    @Test
    void getAllMapsAccountsToResponses() {
        var accountId = UUID.randomUUID();
        var account = org.mockito.Mockito.mock(UserAccount.class);
        when(userAccountRepository.findAll()).thenReturn(List.of(account));
        when(account.getId()).thenReturn(accountId);
        when(account.getName()).thenReturn("Ramy");
        when(account.getEmailAddress()).thenReturn("ramy@example.com");
        when(account.getRole()).thenReturn(EUserRole.REPORTER);

        assertThat(service.getAll()).containsExactly(new UserAccountResponse(
                accountId, "Ramy", "ramy@example.com", EUserRole.REPORTER));
    }

    @Test
    void getDevelopersReturnsOnlyDeveloperAccounts() {
        var developerId = UUID.randomUUID();
        var developer = org.mockito.Mockito.mock(UserAccount.class);
        var reporter = org.mockito.Mockito.mock(UserAccount.class);
        when(userAccountRepository.findAll()).thenReturn(List.of(developer, reporter));
        when(developer.getId()).thenReturn(developerId);
        when(developer.getName()).thenReturn("Ada Lovelace");
        when(developer.getRole()).thenReturn(EUserRole.DEVELOPER);
        when(reporter.getRole()).thenReturn(EUserRole.REPORTER);

        assertThat(service.getDevelopers()).containsExactly(new DeveloperResponse(developerId, "Ada Lovelace"));
    }

    @Test
    void searchUsersReturnsBriefNameMatches() {
        var accountId = UUID.randomUUID();
        var account = org.mockito.Mockito.mock(UserAccount.class);
        when(userAccountRepository.findByNameContainingIgnoreCase("ram"))
                .thenReturn(List.of(account));
        when(account.getId()).thenReturn(accountId);
        when(account.getName()).thenReturn("Ramy");

        assertThat(service.searchUsers(" ram ")).containsExactly(
                new UserAccountBriefResponse(accountId, "Ramy"));
    }

    @Test
    void searchUsersSkipsBlankSearches() {
        assertThat(service.searchUsers(" ")).isEmpty();
    }

    @Test
    void createHashesPasswordAndNormalizesEmail() {
        var request = new CreateUserAccountRequest("Ramy", " Ramy@Example.COM ", "Password123!");
        var accountId = UUID.randomUUID();
        var savedAccount = org.mockito.Mockito.mock(UserAccount.class);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(savedAccount);
        when(savedAccount.getId()).thenReturn(accountId);

        var result = service.create(request);

        var accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).existsByEmailAddress("ramy@example.com");
        verify(userAccountRepository).save(accountCaptor.capture());
        var account = accountCaptor.getValue();
        assertThat(account.getEmailAddress()).isEqualTo("ramy@example.com");
        assertThat(account.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(account.getRole()).isEqualTo(EUserRole.REPORTER);
        assertThat(result.id()).isEqualTo(accountId);
    }

    @Test
    void createRejectsDuplicateNormalizedEmail() {
        var request = new CreateUserAccountRequest("Ramy", "Ramy@Example.COM", "Password123!");
        when(userAccountRepository.existsByEmailAddress("ramy@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void updateRoleChangesAnotherUsersRole() {
        var accountId = UUID.randomUUID();
        var account = new UserAccount("Ramy", "ramy@example.com", "hash", EUserRole.REPORTER);
        when(userAccountRepository.findById(accountId)).thenReturn(Optional.of(account));

        service.updateRole(accountId, new UpdateRoleRequest(EUserRole.DEVELOPER));

        assertThat(account.getRole()).isEqualTo(EUserRole.DEVELOPER);
    }

    @Test
    void updateRoleRejectsDemotingLastAdmin() {
        var accountId = UUID.randomUUID();
        var admin = new UserAccount("Admin", "admin@example.com", "hash", EUserRole.ADMIN);
        when(userAccountRepository.findById(accountId)).thenReturn(Optional.of(admin));
        when(userAccountRepository.findAllByRole(EUserRole.ADMIN)).thenReturn(List.of(admin));

        assertThatThrownBy(() -> service.updateRole(accountId, new UpdateRoleRequest(EUserRole.REPORTER)))
                .isInstanceOf(BusinessRuleConflictException.class)
                .hasMessage("At least one admin account must remain.");
    }

    @Test
    void updateRoleRejectsDemotingDeveloperWithOpenAssignments() {
        var accountId = UUID.randomUUID();
        var developer = new UserAccount("Dev", "dev@example.com", "hash", EUserRole.DEVELOPER);
        when(userAccountRepository.findById(accountId)).thenReturn(Optional.of(developer));
        when(bugReportRepository.existsByAssigneeIdAndStatusNot(accountId, EBugStatus.CLOSED)).thenReturn(true);

        assertThatThrownBy(() -> service.updateRole(accountId, new UpdateRoleRequest(EUserRole.REPORTER)))
                .isInstanceOf(BusinessRuleConflictException.class)
                .hasMessageContaining("open bug report assignments");
    }
}
