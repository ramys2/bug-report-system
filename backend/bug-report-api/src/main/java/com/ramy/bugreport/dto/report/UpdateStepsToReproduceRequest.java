package com.ramy.bugreport.dto.report;

import jakarta.validation.constraints.NotNull;

/**
 * Request body of {@code PATCH /api/reports/{reportId}/steps-to-reproduce}.
 *
 * @param stepsToReproduce the new steps to reproduce; required (may be empty)
 */
public record UpdateStepsToReproduceRequest(@NotNull String stepsToReproduce) {
}
