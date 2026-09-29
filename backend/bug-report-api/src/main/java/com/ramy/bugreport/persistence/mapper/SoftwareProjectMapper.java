package com.ramy.bugreport.persistence.mapper;

import com.ramy.bugreport.domain.SoftwareProject;
import com.ramy.bugreport.persistence.entity.SoftwareProjectEntity;

/**
 * Converts between the domain class {@link com.ramy.bugreport.domain.SoftwareProject} and its JPA entity {@link com.ramy.bugreport.persistence.entity.SoftwareProjectEntity}.
 * Both directions copy every field one to one; a {@code null} input gives a {@code null} result.
 */
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
