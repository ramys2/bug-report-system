package com.ramy.bugreport.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ramy.bugreport.domain.ESupportRequestStatus;
import com.ramy.bugreport.domain.ESupportRequestType;
import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.SupportRequest;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.dto.account.UpdateRoleRequest;
import com.ramy.bugreport.dto.support.CreateSupportRequestRequest;
import com.ramy.bugreport.dto.support.CreateSupportRequestResponse;
import com.ramy.bugreport.dto.support.SupportRequestResponse;
import com.ramy.bugreport.dto.support.UpdateSupportRequestStatusRequest;
import com.ramy.bugreport.dto.support.UpdateSupportRequestStatusResponse;
import com.ramy.bugreport.exception.BusinessRuleConflictException;
import com.ramy.bugreport.exception.ErrorCode;
import com.ramy.bugreport.exception.ResourceNotFoundException;
import com.ramy.bugreport.repository.ISupportRequestRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;
import com.ramy.bugreport.security.UserAccountDetails;

import jakarta.transaction.Transactional;

/** Support creation for non-administrators, and listing/resolution for administrators. */
@Service
public class SupportService {
    private static final Logger logger = LoggerFactory.getLogger(SupportService.class);

    private final ISupportRequestRepository supportRequestRepository;
    private final IUserAccountRepository userAccountRepository;
    private final UserAccountService userAccountService;

    public SupportService(ISupportRequestRepository supportRequestRepository,
            IUserAccountRepository userAccountRepository, UserAccountService userAccountService) {
        this.supportRequestRepository = supportRequestRepository;
        this.userAccountRepository = userAccountRepository;
        this.userAccountService = userAccountService;
    }

    /** Returns requests newest first; requester accounts are loaded in one batch. */
    @PreAuthorize("hasRole('ADMIN')")
    public List<SupportRequestResponse> getAll() {
        var requests = supportRequestRepository.findAll();
        var requesterIds = requests.stream().map(SupportRequest::getRequesterId).distinct().toList();
        var requesters = userAccountRepository.findAllById(requesterIds).stream()
                .collect(Collectors.toMap(UserAccount::getId, Function.identity()));
        logger.debug("event=supportRequestsLookup count={}", requests.size());
        return requests.stream().map(request -> {
            var requester = requesters.get(request.getRequesterId());
            if (requester == null) {
                throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "The requester account does not exist.");
            }
            return SupportRequestResponse.from(request, requester);
        }).toList();
    }

    /**
     * Saves a validated request as PENDING. Caller identity comes from the security context,
     * so neither HTTP clients nor other service callers can supply a different requester.
     * The URL rule and this method both require a non-administrator role.
     */
    @Transactional
    @PreAuthorize("hasAnyRole('REPORTER', 'DEVELOPER') and !hasRole('ADMIN')")
    public CreateSupportRequestResponse create(CreateSupportRequestRequest request) {
        UserAccount requester = currentRequester();

        if (request.type() == ESupportRequestType.ROLE_CHANGE) {
            // This is the only eligible change in the existing REPORTER < DEVELOPER < ADMIN hierarchy.
            if (requester.getRole() != EUserRole.REPORTER || request.requestedRole() != EUserRole.DEVELOPER) {
                throw new BusinessRuleConflictException(ErrorCode.SUPPORT_ROLE_NOT_ALLOWED,
                        "Only a Reporter can request the Developer role.");
            }
            if (supportRequestRepository.existsPendingRoleChange(requester.getId(), request.requestedRole())) {
                throw new BusinessRuleConflictException(ErrorCode.SUPPORT_REQUEST_ALREADY_PENDING,
                        "A pending request for this role already exists.");
            }
        }

        String description = request.description() == null || request.description().isBlank()
                ? null : request.description().trim();
        var supportRequest = new SupportRequest(requester.getId(), request.type(), request.requestedRole(),
                description, LocalDateTime.now());
        supportRequest = supportRequestRepository.save(supportRequest);
        logger.info("event=supportRequestCreatePrepared requestId={} requesterId={} type={} requestedRole={} transaction=pendingCommit",
                supportRequest.getId(), requester.getId(), supportRequest.getType(), supportRequest.getRequestedRole());
        return new CreateSupportRequestResponse("SUPPORT_REQUEST_CREATED", "Support request created successfully.");
    }

    /**
     * Locks before checking PENDING so concurrent decisions cannot resolve the same request twice.
     * Role updates join this transaction: a failure in either save rolls back both changes.
     * Invalid/outdated approvals return a conflict and leave the request pending for review/rejection.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UpdateSupportRequestStatusResponse updateStatus(UUID requestId, UpdateSupportRequestStatusRequest request) {
        if (request.status() != ESupportRequestStatus.APPROVED && request.status() != ESupportRequestStatus.REJECTED) {
            throw new BusinessRuleConflictException(ErrorCode.SUPPORT_INVALID_RESOLUTION_STATUS,
                    "A resolution must be APPROVED or REJECTED.");
        }
        var supportRequest = supportRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SUPPORT_REQUEST_NOT_FOUND,
                        "Support request with id=%s does not exist.".formatted(requestId)));
        if (supportRequest.getStatus() != ESupportRequestStatus.PENDING) {
            throw new BusinessRuleConflictException(ErrorCode.SUPPORT_REQUEST_ALREADY_RESOLVED,
                    "The support request has already been resolved.");
        }
        if (request.status() == ESupportRequestStatus.APPROVED) {
            if (supportRequest.getRequesterId().equals(authenticatedAccountId())) {
                throw new AccessDeniedException("You cannot approve your own support request.");
            }
            if (supportRequest.getType() == ESupportRequestType.ROLE_CHANGE) {
                var requester = userAccountRepository.findByIdForUpdate(supportRequest.getRequesterId())
                        .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND,
                                "The requester account does not exist."));
                if (requester.getRole() != EUserRole.REPORTER || supportRequest.getRequestedRole() != EUserRole.DEVELOPER) {
                    throw new BusinessRuleConflictException(ErrorCode.SUPPORT_ROLE_CHANGE_OUTDATED,
                            "The requested role change is no longer valid for this account.");
                }
                userAccountService.updateRole(requester.getId(), new UpdateRoleRequest(supportRequest.getRequestedRole()));
            }
        }
        var resolved = new SupportRequest(supportRequest.getId(), supportRequest.getRequesterId(), supportRequest.getType(),
                supportRequest.getRequestedRole(), supportRequest.getDescription(), request.status(),
                supportRequest.getCreatedAt(), LocalDateTime.now());
        supportRequestRepository.save(resolved);
        logger.info("event=supportRequestResolvePrepared requestId={} requesterId={} status={} transaction=pendingCommit",
                requestId, resolved.getRequesterId(), resolved.getStatus());
        return new UpdateSupportRequestStatusResponse("SUPPORT_REQUEST_RESOLVED", "Support request resolved successfully.");
    }

    /** Rejects principals that do not represent an authenticated application account. */
    private UUID authenticatedAccountId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserAccountDetails principal)) {
            throw new AccessDeniedException("An authenticated application account is required.");
        }
        return principal.getId();
    }

    /** Checks the stored role too, including when this service is called outside an HTTP request. */
    private UserAccount currentRequester() {
        var account = userAccountRepository.findById(authenticatedAccountId())
                .orElseThrow(() -> new AccessDeniedException("The authenticated account no longer exists."));
        if (account.getRole() != EUserRole.REPORTER && account.getRole() != EUserRole.DEVELOPER) {
            throw new AccessDeniedException("Administrators cannot create support requests.");
        }
        return account;
    }
}
