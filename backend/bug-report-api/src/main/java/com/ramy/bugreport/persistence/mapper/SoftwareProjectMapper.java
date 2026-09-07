package com.ramy.bugreport.persistence.mapper;

import com.ramy.bugreport.domain.SoftwareProject;
import com.ramy.bugreport.persistence.entity.SoftwareProjectEntity;

public final class SoftwareProjectMapper {
    private SoftwareProjectMapper() {
    }

    public static SoftwareProjectEntity toEntity(SoftwareProject domain) {
        if (domain == null) {
            return null;
        }
        SoftwareProjectEntity entity = new SoftwareProjectEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        return entity;
    }

    public static SoftwareProject toDomain(SoftwareProjectEntity entity) {
        if (entity == null) {
            return null;
        }
        return new SoftwareProject(entity.getId(), entity.getName(), entity.getDescription());
    }
}
