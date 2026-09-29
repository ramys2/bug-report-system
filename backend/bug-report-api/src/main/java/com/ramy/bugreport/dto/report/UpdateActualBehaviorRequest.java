package com.ramy.bugreport.dto.report;

import jakarta.validation.constraints.NotNull;

/**
 * Request body of {@code PATCH /api/reports/{reportId}/actual-behavior}.
 *
 * @param actualBehavior the new actual behavior; required (may be empty)
 */
public record UpdateActualBehaviorRequest(@NotNull String actualBehavior) {
}
