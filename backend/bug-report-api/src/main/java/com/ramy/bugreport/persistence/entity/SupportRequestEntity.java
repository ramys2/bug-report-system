package com.ramy.bugreport.persistence.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.ramy.bugreport.domain.ESupportRequestStatus;
import com.ramy.bugreport.domain.ESupportRequestType;
import com.ramy.bugreport.domain.EUserRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** JPA counterpart of SupportRequest. The requester uses a UUID column, like a comment's author. */
@Entity
@Table(name = "support_request")
public class SupportRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "requester_id", nullable = false, length = 36)
    private UUID requesterId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type", nullable = false, length = 32)
    private ESupportRequestType type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "requested_role", length = 32)
    private EUserRole requestedRole;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 32)
    private ESupportRequestStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(UUID requesterId) {
        this.requesterId = requesterId;
    }

    public ESupportRequestType getType() {
        return type;
    }

    public void setType(ESupportRequestType type) {
        this.type = type;
    }

    public EUserRole getRequestedRole() {
        return requestedRole;
    }

    public void setRequestedRole(EUserRole requestedRole) {
        this.requestedRole = requestedRole;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ESupportRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ESupportRequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
