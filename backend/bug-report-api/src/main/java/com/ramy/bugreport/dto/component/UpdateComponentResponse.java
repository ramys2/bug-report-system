package com.ramy.bugreport.dto.component;

import java.util.UUID;

/**
 * Response of the {@code PATCH /api/components/{componentId}/...} endpoints.
 *
 * @param id id of the component
 * @param message confirmation text
 */
public record UpdateComponentResponse(UUID id, String message) {
}
