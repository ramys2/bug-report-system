package com.ramy.bugreport.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ramy.bugreport.domain.ESupportRequestType;
import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.SupportRequest;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.dto.support.CreateSupportRequestRequest;
import com.ramy.bugreport.dto.support.CreateSupportRequestResponse;
import com.ramy.bugreport.exception.BusinessRuleConflictException;
import com.ramy.bugreport.exception.ErrorCode;
import com.ramy.bugreport.repository.ISupportRequestRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;
import com.ramy.bugreport.security.UserAccountDetails;

import jakarta.transaction.Transactional;

/** Creates support requests on behalf of the signed-in, non-administrator account. */
@Service
public class SupportService {
    private static final Logger logger = LoggerFactory.getLogger(SupportService.class);

    private final ISupportRequestRepository supportRequestRepository;
    private final IUserAccountRepository userAccountRepository;

    public SupportService(ISupportRequestRepository supportRequestRepository,
            IUserAccountRepository userAccountRepository) {
        this.supportRequestRepository = supportRequestRepository;
        this.userAccountRepository = userAccountRepository;
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

    /** Checks the stored role too, including when this service is called outside an HTTP request. */
    private UserAccount currentRequester() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserAccountDetails principal)) {
            throw new AccessDeniedException("An authenticated application account is required.");
        }
        var account = userAccountRepository.findById(principal.getId())
                .orElseThrow(() -> new AccessDeniedException("The authenticated account no longer exists."));
        if (account.getRole() != EUserRole.REPORTER && account.getRole() != EUserRole.DEVELOPER) {
            throw new AccessDeniedException("Administrators cannot create support requests.");
        }
        return account;
    }
}
