package com.ramy.bugreport.dto.report;

import jakarta.validation.constraints.NotNull;

/**
 * Request body of {@code PATCH /api/reports/{reportId}/expected-behavior}.
 *
 * @param expectedBehavior the new expected behavior; required (may be empty)
 */
public record UpdateExpectedBehaviorRequest(@NotNull String expectedBehavior) {
}
