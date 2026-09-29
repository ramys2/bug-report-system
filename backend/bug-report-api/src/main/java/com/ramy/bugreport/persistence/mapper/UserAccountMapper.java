package com.ramy.bugreport.persistence.mapper;

import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.persistence.entity.UserAccountEntity;

/**
 * Converts between the domain class {@link com.ramy.bugreport.domain.UserAccount} and its JPA entity {@link com.ramy.bugreport.persistence.entity.UserAccountEntity}.
 * Both directions copy every field one to one; a {@code null} input gives a {@code null} result.
 */
public final class UserAccountMapper {
    private UserAccountMapper() {
    }

    public static UserAccountEntity toEntity(UserAccount domain) {
        if (domain == null) {
            return null;
        }
        UserAccountEntity entity = new UserAccountEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setEmailAddress(domain.getEmailAddress());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setRole(domain.getRole());
        return entity;
    }

    public static UserAccount toDomain(UserAccountEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UserAccount(
                entity.getId(), entity.getName(), entity.getEmailAddress(),
                entity.getPasswordHash(), entity.getRole());
    }
}
