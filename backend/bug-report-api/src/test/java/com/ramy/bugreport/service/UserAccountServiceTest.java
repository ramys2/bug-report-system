package com.ramy.bugreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.dto.account.CreateUserAccountRequest;
import com.ramy.bugreport.dto.account.DeveloperResponse;
import com.ramy.bugreport.dto.account.UserAccountResponse;
import com.ramy.bugreport.repository.IUserAccountRepository;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private IUserAccountRepository userAccountRepository;

    private UserAccountService service;

    @BeforeEach
    void setUp() {
        service = new UserAccountService(userAccountRepository);
    }

    @Test
    void getAllMapsAccountsToResponses() {
        var accountId = UUID.randomUUID();
        var userAccount = org.mockito.Mockito.mock(UserAccount.class);
        when(userAccountRepository.findAll()).thenReturn(List.of(userAccount));
        when(userAccount.getId()).thenReturn(accountId);
        when(userAccount.getName()).thenReturn("Ramy");
        when(userAccount.getEmailAddress()).thenReturn("ramy@example.com");
        when(userAccount.getRole()).thenReturn(EUserRole.REPORTER);

        var result = service.getAll();

        assertThat(result).containsExactly(new UserAccountResponse(
                accountId, "Ramy", "ramy@example.com", EUserRole.REPORTER));
    }

    @Test
    void getDevelopersReturnsOnlyDeveloperAccounts() {
        var developerId = UUID.randomUUID();
        var reporterId = UUID.randomUUID();
        var developer = org.mockito.Mockito.mock(UserAccount.class);
        var reporter = org.mockito.Mockito.mock(UserAccount.class);
        when(userAccountRepository.findAll()).thenReturn(List.of(developer, reporter));
        when(developer.getId()).thenReturn(developerId);
        when(developer.getName()).thenReturn("Ada Lovelace");
        when(developer.getRole()).thenReturn(EUserRole.DEVELOPER);
        when(reporter.getRole()).thenReturn(EUserRole.REPORTER);

        var result = service.getDevelopers();

        assertThat(result).containsExactly(new DeveloperResponse(developerId, "Ada Lovelace"));
    }

    @Test
    void createBuildsAndSavesReporterAccount() {
        var request = new CreateUserAccountRequest("Ramy", "ramy@example.com", "Password123!");
        var accountId = UUID.randomUUID();
        var savedAccount = org.mockito.Mockito.mock(UserAccount.class);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(savedAccount);
        when(savedAccount.getId()).thenReturn(accountId);

        var result = service.create(request);

        var accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(accountCaptor.capture());
        var account = accountCaptor.getValue();
        assertThat(account.getName()).isEqualTo(request.username());
        assertThat(account.getEmailAddress()).isEqualTo(request.email());
        assertThat(account.getPasswordHash()).isEqualTo(request.password());
        assertThat(account.getRole()).isEqualTo(EUserRole.REPORTER);
        assertThat(result.id()).isEqualTo(accountId);
        assertThat(result.message()).isEqualTo("Successfully created!");
    }
}
