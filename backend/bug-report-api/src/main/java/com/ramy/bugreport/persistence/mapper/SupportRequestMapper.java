package com.ramy.bugreport.persistence.mapper;

import com.ramy.bugreport.domain.SupportRequest;
import com.ramy.bugreport.persistence.entity.SupportRequestEntity;

/** Converts every stored support-request field between domain and JPA objects. */
public final class SupportRequestMapper {
    private SupportRequestMapper() {
    }

    public static SupportRequestEntity toEntity(SupportRequest request) {
        if (request == null) {
            return null;
        }
        var entity = new SupportRequestEntity();
        entity.setId(request.getId());
        entity.setRequesterId(request.getRequesterId());
        entity.setType(request.getType());
        entity.setRequestedRole(request.getRequestedRole());
        entity.setDescription(request.getDescription());
        entity.setStatus(request.getStatus());
        entity.setCreatedAt(request.getCreatedAt());
        entity.setResolvedAt(request.getResolvedAt());
        return entity;
    }

    public static SupportRequest toDomain(SupportRequestEntity entity) {
        if (entity == null) {
            return null;
        }
        return new SupportRequest(entity.getId(), entity.getRequesterId(), entity.getType(),
                entity.getRequestedRole(), entity.getDescription(), entity.getStatus(),
                entity.getCreatedAt(), entity.getResolvedAt());
    }
}
