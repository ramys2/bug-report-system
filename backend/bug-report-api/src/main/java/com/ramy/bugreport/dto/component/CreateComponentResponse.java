package com.ramy.bugreport.dto.component;

import java.util.UUID;

/**
 * Response of {@code POST /api/components}.
 *
 * @param id id of the new component
 * @param message confirmation text
 */
public record CreateComponentResponse(UUID id, String message) {
}
