package com.ramy.bugreport.dto.report;

import jakarta.validation.constraints.NotNull;

/**
 * Request body of {@code PATCH /api/reports/{reportId}/description}.
 *
 * @param description the new description; required (may be empty)
 */
public record UpdateDescriptionRequest(@NotNull String description) {
}
