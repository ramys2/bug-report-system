package com.ramy.bugreport.dto.component;

import java.util.UUID;

import com.ramy.bugreport.domain.Component;
import com.ramy.bugreport.domain.UserAccount;

/**
 * A component in {@code GET /api/components}.
 *
 * @param id component id
 * @param name display name
 * @param description description; may be null
 * @param responsibleUserName display name of the responsible user; null if that user cannot be found
 */
public record ComponentResponse(
        UUID id,
        String name,
        String description,
        String responsibleUserName
) {

    public static ComponentResponse from(Component component, UserAccount responsibleUser) {
        return new ComponentResponse(
                component.getId(),
                component.getName(),
                component.getDescription(),
                responsibleUser == null ? null : responsibleUser.getName());
    }
}
