package com.ramy.bugreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ramy.bugreport.domain.ESupportRequestStatus;
import com.ramy.bugreport.domain.ESupportRequestType;
import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.SupportRequest;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.dto.support.CreateSupportRequestRequest;
import com.ramy.bugreport.exception.BusinessRuleConflictException;
import com.ramy.bugreport.exception.ErrorCode;
import com.ramy.bugreport.repository.ISupportRequestRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;
import com.ramy.bugreport.security.UserAccountDetails;

@ExtendWith(MockitoExtension.class)
class SupportServiceTest {
    @Mock private ISupportRequestRepository requests;
    @Mock private IUserAccountRepository users;
    private SupportService service;

    @BeforeEach
    void setUp() {
        service = new SupportService(requests, users);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void reporterCreatesPendingDeveloperRequestWithoutChangingTheirRole() {
        var reporter = signIn(EUserRole.REPORTER);
        when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime before = LocalDateTime.now();
        var response = service.create(new CreateSupportRequestRequest(ESupportRequestType.ROLE_CHANGE,
                EUserRole.DEVELOPER, " Please grant access "));

        var captured = ArgumentCaptor.forClass(SupportRequest.class);
        verify(requests).save(captured.capture());
        var saved = captured.getValue();
        assertThat(saved.getRequesterId()).isEqualTo(reporter.getId());
        assertThat(saved.getType()).isEqualTo(ESupportRequestType.ROLE_CHANGE);
        assertThat(saved.getRequestedRole()).isEqualTo(EUserRole.DEVELOPER);
        assertThat(saved.getDescription()).isEqualTo("Please grant access");
        assertThat(saved.getStatus()).isEqualTo(ESupportRequestStatus.PENDING);
        assertThat(saved.getCreatedAt()).isBetween(before, LocalDateTime.now());
        assertThat(saved.getResolvedAt()).isNull();
        assertThat(response.code()).isEqualTo("SUPPORT_REQUEST_CREATED");
        assertThat(response.message()).isEqualTo("Support request created successfully.");
        assertThat(reporter.getRole()).isEqualTo(EUserRole.REPORTER);
        verify(users, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({"REPORTER,REPORTER", "REPORTER,ADMIN", "DEVELOPER,REPORTER", "DEVELOPER,DEVELOPER", "DEVELOPER,ADMIN"})
    void currentLowerAndAdministratorRolesCannotBeRequested(EUserRole currentRole, EUserRole requestedRole) {
        signIn(currentRole);

        assertThatThrownBy(() -> service.create(new CreateSupportRequestRequest(
                ESupportRequestType.ROLE_CHANGE, requestedRole, null)))
                .isInstanceOfSatisfying(BusinessRuleConflictException.class,
                        error -> assertThat(error.getCode()).isEqualTo(ErrorCode.SUPPORT_ROLE_NOT_ALLOWED));
        verifyNoInteractions(requests);
    }

    @Test
    void pendingRoleChangeCannotBeDuplicated() {
        var reporter = signIn(EUserRole.REPORTER);
        when(requests.existsPendingRoleChange(reporter.getId(), EUserRole.DEVELOPER)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateSupportRequestRequest(
                ESupportRequestType.ROLE_CHANGE, EUserRole.DEVELOPER, null)))
                .isInstanceOfSatisfying(BusinessRuleConflictException.class,
                        error -> assertThat(error.getCode()).isEqualTo(ErrorCode.SUPPORT_REQUEST_ALREADY_PENDING));
        verify(requests, never()).save(any());
    }

    @Test
    void developerCanCreateGeneralRequestWithTrimmedDescription() {
        var developer = signIn(EUserRole.DEVELOPER);
        when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new CreateSupportRequestRequest(ESupportRequestType.OTHER, null, " Help please "));

        var captured = ArgumentCaptor.forClass(SupportRequest.class);
        verify(requests).save(captured.capture());
        var saved = captured.getValue();
        assertThat(saved.getRequesterId()).isEqualTo(developer.getId());
        assertThat(saved.getType()).isEqualTo(ESupportRequestType.OTHER);
        assertThat(saved.getDescription()).isEqualTo("Help please");
        assertThat(saved.getRequestedRole()).isNull();
        verify(requests, never()).existsPendingRoleChange(any(), any());
        verify(users, never()).save(any());
    }

    @Test
    void creationFailsClosedWithoutAnApplicationPrincipal() {
        assertThatThrownBy(() -> service.create(new CreateSupportRequestRequest(ESupportRequestType.OTHER, null, "Help")))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(users, requests);
    }

    @Test
    void currentDatabaseRoleOverridesStaleReporterAuthority() {
        var account = signIn(EUserRole.REPORTER);
        account.setRole(EUserRole.ADMIN);

        assertThatThrownBy(() -> service.create(new CreateSupportRequestRequest(ESupportRequestType.OTHER, null, "Help")))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(requests);
    }

    @Test
    void deletedAccountCannotCreateRequests() {
        var account = signIn(EUserRole.REPORTER);
        when(users.findById(account.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateSupportRequestRequest(ESupportRequestType.OTHER, null, "Help")))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(requests);
    }

    private UserAccount signIn(EUserRole role) {
        var account = new UserAccount(UUID.randomUUID(), "User", "user@example.com", "hash", role);
        var principal = new UserAccountDetails(account);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(users.findById(account.getId())).thenReturn(Optional.of(account));
        return account;
    }
}
