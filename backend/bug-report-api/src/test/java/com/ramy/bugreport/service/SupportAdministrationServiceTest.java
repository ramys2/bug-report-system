package com.ramy.bugreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import com.ramy.bugreport.dto.account.UpdateRoleRequest;
import com.ramy.bugreport.dto.support.UpdateSupportRequestStatusRequest;
import com.ramy.bugreport.exception.BusinessRuleConflictException;
import com.ramy.bugreport.exception.ErrorCode;
import com.ramy.bugreport.exception.ResourceNotFoundException;
import com.ramy.bugreport.repository.ISupportRequestRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;
import com.ramy.bugreport.security.UserAccountDetails;

@ExtendWith(MockitoExtension.class)
class SupportAdministrationServiceTest {
    @Mock private ISupportRequestRepository requests;
    @Mock private IUserAccountRepository users;
    @Mock private UserAccountService userAccountService;
    private SupportService service;
    private final UUID requesterId = UUID.randomUUID();
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 10, 25, 10, 0);

    @BeforeEach
    void setUp() {
        service = new SupportService(requests, users, userAccountService);
        var principal = new UserAccountDetails(new UserAccount(UUID.randomUUID(), "Admin", "admin@example.com", "hash", EUserRole.ADMIN));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listingLoadsRequesterDetailsInOneBatchAndPreservesOrder() {
        var first = pending(ESupportRequestType.OTHER, null);
        var second = pending(ESupportRequestType.ROLE_CHANGE, EUserRole.DEVELOPER);
        when(requests.findAll()).thenReturn(List.of(first, second));
        when(users.findAllById(List.of(requesterId))).thenReturn(List.of(account(EUserRole.REPORTER)));

        var response = service.getAll();

        assertThat(response).extracting(item -> item.id()).containsExactly(first.getId(), second.getId());
        assertThat(response).allSatisfy(item -> {
            assertThat(item.requesterId()).isEqualTo(requesterId);
            assertThat(item.requesterName()).isEqualTo("Requester");
            assertThat(item.status()).isEqualTo(ESupportRequestStatus.PENDING);
        });
        verify(users).findAllById(List.of(requesterId));
    }

    @Test
    void approvalRevalidatesStoredRoleAndUsesExistingRoleManagement() {
        var supportRequest = pending(ESupportRequestType.ROLE_CHANGE, EUserRole.DEVELOPER);
        when(requests.findByIdForUpdate(supportRequest.getId())).thenReturn(Optional.of(supportRequest));
        when(users.findByIdForUpdate(requesterId)).thenReturn(Optional.of(account(EUserRole.REPORTER)));

        var before = LocalDateTime.now();
        var response = service.updateStatus(supportRequest.getId(), status(ESupportRequestStatus.APPROVED));

        verify(userAccountService).updateRole(requesterId, new UpdateRoleRequest(EUserRole.DEVELOPER));
        var captured = ArgumentCaptor.forClass(SupportRequest.class);
        verify(requests).save(captured.capture());
        var saved = captured.getValue();
        assertThat(saved.getStatus()).isEqualTo(ESupportRequestStatus.APPROVED);
        assertThat(saved.getResolvedAt()).isBetween(before, LocalDateTime.now());
        assertThat(saved.getId()).isEqualTo(supportRequest.getId());
        assertThat(saved.getCreatedAt()).isEqualTo(createdAt);
        assertThat(saved.getRequestedRole()).isEqualTo(EUserRole.DEVELOPER);
        assertThat(response.code()).isEqualTo("SUPPORT_REQUEST_RESOLVED");
    }

    @ParameterizedTest
    @EnumSource(value = EUserRole.class, names = {"DEVELOPER", "ADMIN"})
    void outdatedRoleChangesCannotBeApproved(EUserRole currentRole) {
        var supportRequest = pending(ESupportRequestType.ROLE_CHANGE, EUserRole.DEVELOPER);
        when(requests.findByIdForUpdate(supportRequest.getId())).thenReturn(Optional.of(supportRequest));
        when(users.findByIdForUpdate(requesterId)).thenReturn(Optional.of(account(currentRole)));

        assertThatThrownBy(() -> service.updateStatus(supportRequest.getId(), status(ESupportRequestStatus.APPROVED)))
                .isInstanceOfSatisfying(BusinessRuleConflictException.class,
                        error -> assertThat(error.getCode()).isEqualTo(ErrorCode.SUPPORT_ROLE_CHANGE_OUTDATED));
        verifyNoInteractions(userAccountService);
        verify(requests, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = EUserRole.class, names = {"REPORTER", "ADMIN"})
    void invalidTargetRolesCannotBeApproved(EUserRole targetRole) {
        var supportRequest = pending(ESupportRequestType.ROLE_CHANGE, targetRole);
        when(requests.findByIdForUpdate(supportRequest.getId())).thenReturn(Optional.of(supportRequest));
        when(users.findByIdForUpdate(requesterId)).thenReturn(Optional.of(account(EUserRole.REPORTER)));

        assertThatThrownBy(() -> service.updateStatus(supportRequest.getId(), status(ESupportRequestStatus.APPROVED)))
                .isInstanceOf(BusinessRuleConflictException.class);
        verifyNoInteractions(userAccountService);
        verify(requests, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(ESupportRequestType.class)
    void rejectionNeverChangesTheUsersRole(ESupportRequestType type) {
        var supportRequest = pending(type, type == ESupportRequestType.ROLE_CHANGE ? EUserRole.DEVELOPER : null);
        when(requests.findByIdForUpdate(supportRequest.getId())).thenReturn(Optional.of(supportRequest));

        service.updateStatus(supportRequest.getId(), status(ESupportRequestStatus.REJECTED));

        var captured = ArgumentCaptor.forClass(SupportRequest.class);
        verify(requests).save(captured.capture());
        assertThat(captured.getValue().getStatus()).isEqualTo(ESupportRequestStatus.REJECTED);
        assertThat(captured.getValue().getResolvedAt()).isNotNull();
        verifyNoInteractions(users, userAccountService);
    }

    @Test
    void generalRequestApprovalDoesNotChangeRoles() {
        var supportRequest = pending(ESupportRequestType.OTHER, null);
        when(requests.findByIdForUpdate(supportRequest.getId())).thenReturn(Optional.of(supportRequest));

        service.updateStatus(supportRequest.getId(), status(ESupportRequestStatus.APPROVED));

        verifyNoInteractions(users, userAccountService);
        verify(requests).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = ESupportRequestStatus.class, names = {"APPROVED", "REJECTED"})
    void resolvedRequestsCannotBeProcessedAgain(ESupportRequestStatus resolvedStatus) {
        var supportRequest = new SupportRequest(UUID.randomUUID(), requesterId, ESupportRequestType.OTHER,
                null, "Help", resolvedStatus, createdAt, createdAt.plusHours(1));
        when(requests.findByIdForUpdate(supportRequest.getId())).thenReturn(Optional.of(supportRequest));

        assertThatThrownBy(() -> service.updateStatus(supportRequest.getId(), status(ESupportRequestStatus.REJECTED)))
                .isInstanceOfSatisfying(BusinessRuleConflictException.class,
                        error -> assertThat(error.getCode()).isEqualTo(ErrorCode.SUPPORT_REQUEST_ALREADY_RESOLVED));
        verify(requests, never()).save(any());
    }

    @Test
    void pendingCannotBeUsedAsAResolution() {
        assertThatThrownBy(() -> service.updateStatus(UUID.randomUUID(), status(ESupportRequestStatus.PENDING)))
                .isInstanceOfSatisfying(BusinessRuleConflictException.class,
                        error -> assertThat(error.getCode()).isEqualTo(ErrorCode.SUPPORT_INVALID_RESOLUTION_STATUS));
        verifyNoInteractions(requests, users, userAccountService);
    }

    @Test
    void unknownRequestReturnsNotFound() {
        var id = UUID.randomUUID();
        when(requests.findByIdForUpdate(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateStatus(id, status(ESupportRequestStatus.REJECTED)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void administratorCannotApproveTheirOwnHistoricalRequest() {
        var principal = (UserAccountDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var supportRequest = new SupportRequest(UUID.randomUUID(), principal.getId(), ESupportRequestType.OTHER,
                null, "Help", ESupportRequestStatus.PENDING, createdAt, null);
        when(requests.findByIdForUpdate(supportRequest.getId())).thenReturn(Optional.of(supportRequest));

        assertThatThrownBy(() -> service.updateStatus(supportRequest.getId(), status(ESupportRequestStatus.APPROVED)))
                .isInstanceOf(AccessDeniedException.class);
        verify(requests, never()).save(any());
    }

    private UserAccount account(EUserRole role) {
        return new UserAccount(requesterId, "Requester", "requester@example.com", "hash", role);
    }

    private SupportRequest pending(ESupportRequestType type, EUserRole role) {
        return new SupportRequest(UUID.randomUUID(), requesterId, type, role, "Help",
                ESupportRequestStatus.PENDING, createdAt, null);
    }

    private UpdateSupportRequestStatusRequest status(ESupportRequestStatus status) {
        return new UpdateSupportRequestStatusRequest(status);
    }
}
