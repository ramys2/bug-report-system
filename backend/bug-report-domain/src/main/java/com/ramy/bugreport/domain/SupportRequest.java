package com.ramy.bugreport.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/** A request for administrator assistance, linked to the existing account by its id. */
public class SupportRequest {
    private final UUID id;
    private final UUID requesterId;
    private final ESupportRequestType type;
    private final EUserRole requestedRole;
    private final String description;
    private final ESupportRequestStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime resolvedAt;

    /** Creates a pending request. The id is generated when it is saved. */
    public SupportRequest(UUID requesterId, ESupportRequestType type, EUserRole requestedRole,
            String description, LocalDateTime createdAt) {
        this(null, requesterId, type, requestedRole, description, ESupportRequestStatus.PENDING, createdAt, null);
    }

    /** Reconstitutes a request from persistence. */
    public SupportRequest(UUID id, UUID requesterId, ESupportRequestType type, EUserRole requestedRole,
            String description, ESupportRequestStatus status, LocalDateTime createdAt, LocalDateTime resolvedAt) {
        this.id = id;
        this.requesterId = requesterId;
        this.type = type;
        this.requestedRole = requestedRole;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public ESupportRequestType getType() {
        return type;
    }

    public EUserRole getRequestedRole() {
        return requestedRole;
    }

    public String getDescription() {
        return description;
    }

    public ESupportRequestStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }
}
