package com.ramy.bugreport.dto.support;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ramy.bugreport.domain.ESupportRequestStatus;
import com.ramy.bugreport.domain.ESupportRequestType;
import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.SupportRequest;
import com.ramy.bugreport.domain.UserAccount;

/** Administrator listing item. Includes the requester's name without exposing account credentials. */
public record SupportRequestResponse(
        UUID id,
        UUID requesterId,
        String requesterName,
        ESupportRequestType type,
        EUserRole requestedRole,
        String description,
        ESupportRequestStatus status,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
    public static SupportRequestResponse from(SupportRequest request, UserAccount requester) {
        return new SupportRequestResponse(request.getId(), request.getRequesterId(), requester.getName(),
                request.getType(), request.getRequestedRole(), request.getDescription(), request.getStatus(),
                request.getCreatedAt(), request.getResolvedAt());
    }
}
