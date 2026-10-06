package com.ramy.bugreport.persistence.mapper;

import com.ramy.bugreport.domain.Component;
import com.ramy.bugreport.persistence.entity.ComponentEntity;

/**
 * Converts between the domain class {@link com.ramy.bugreport.domain.Component} and its JPA entity {@link com.ramy.bugreport.persistence.entity.ComponentEntity}.
 * Both directions copy every field one to one; a {@code null} input gives a {@code null} result.
 */
public final class ComponentMapper {
    private ComponentMapper() {
    }

    public static ComponentEntity toEntity(Component domain) {
        if (domain == null) {
            return null;
        }
        ComponentEntity entity = new ComponentEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setResponsibleUserId(domain.getResponsibleUserId());
        entity.setProjectId(domain.getProjectId());
        return entity;
    }

    public static Component toDomain(ComponentEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Component(entity.getId(), entity.getName(), entity.getDescription(),
                entity.getResponsibleUserId(), entity.getProjectId());
    }
}
