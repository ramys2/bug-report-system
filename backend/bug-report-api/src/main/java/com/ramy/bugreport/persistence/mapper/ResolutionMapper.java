package com.ramy.bugreport.persistence.mapper;

import com.ramy.bugreport.domain.Resolution;
import com.ramy.bugreport.persistence.entity.ResolutionEntity;

public final class ResolutionMapper {
    private ResolutionMapper() {
    }

    public static ResolutionEntity toEntity(Resolution domain) {
        if (domain == null) {
            return null;
        }
        ResolutionEntity entity = new ResolutionEntity();
        entity.setId(domain.getId());
        entity.setDescription(domain.getDescription());
        entity.setResolvedAt(domain.getResolvedAt());
        entity.setFixedVersion(domain.getFixedVersion());
        entity.setCommitUrl(domain.getCommitUrl());
        return entity;
    }

    public static Resolution toDomain(ResolutionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Resolution(
                entity.getId(), entity.getDescription(), entity.getResolvedAt(),
                entity.getFixedVersion(), entity.getCommitUrl());
    }
}
