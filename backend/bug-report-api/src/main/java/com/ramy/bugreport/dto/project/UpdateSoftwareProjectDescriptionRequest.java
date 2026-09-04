package com.ramy.bugreport.dto.project;

import jakarta.validation.constraints.NotNull;

public record UpdateSoftwareProjectDescriptionRequest(@NotNull String description) {
}
