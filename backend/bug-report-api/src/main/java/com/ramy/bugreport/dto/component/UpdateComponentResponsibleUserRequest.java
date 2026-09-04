package com.ramy.bugreport.dto.component;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UpdateComponentResponsibleUserRequest(@NotNull UUID responsibleUserId) {
}
