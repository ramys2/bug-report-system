package com.ramy.bugreport.dto.component;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Request body of {@code PATCH /api/components/{componentId}/responsibleUserId}.
 *
 * @param responsibleUserId id of the new responsible user; required
 */
public record UpdateComponentResponsibleUserRequest(@NotNull UUID responsibleUserId) {
}
