package com.ramy.bugreport.dto.component;

import jakarta.validation.constraints.NotBlank;

public record UpdateComponentNameRequest(@NotBlank String name) {
}
