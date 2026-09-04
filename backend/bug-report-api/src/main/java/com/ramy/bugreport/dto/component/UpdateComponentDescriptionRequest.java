package com.ramy.bugreport.dto.component;

import jakarta.validation.constraints.NotNull;

public record UpdateComponentDescriptionRequest(@NotNull String description) {
}
