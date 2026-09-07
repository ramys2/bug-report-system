package com.ramy.bugreport.persistence.mapper;

import com.ramy.bugreport.domain.Component;
import com.ramy.bugreport.persistence.entity.ComponentEntity;

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
        return entity;
    }

    public static Component toDomain(ComponentEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Component(entity.getId(), entity.getName(), entity.getDescription(), entity.getResponsibleUserId());
    }
}
