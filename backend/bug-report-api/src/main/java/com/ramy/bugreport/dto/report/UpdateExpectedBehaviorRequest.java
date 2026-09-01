package com.ramy.bugreport.dto.report;

import jakarta.validation.constraints.NotNull;

public record UpdateExpectedBehaviorRequest(@NotNull String expectedBehavior) {
}
