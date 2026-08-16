package com.ramy.bugreport.dto.project;

import jakarta.validation.constraints.NotBlank;

public record CreateSoftwareProjectRequest(
        @NotBlank String name,
        String description
) {
}
