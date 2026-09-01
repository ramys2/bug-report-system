package com.ramy.bugreport.dto.report;

import jakarta.validation.constraints.NotNull;

public record UpdateStepsToReproduceRequest(@NotNull String stepsToReproduce) {
}
