package com.ramy.bugreport.dto.report;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UpdateComponentRequest(@NotNull UUID componentId) {
}
