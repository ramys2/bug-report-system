package com.ramy.bugreport.dto.project;

import jakarta.validation.constraints.NotBlank;

public record UpdateSoftwareProjectNameRequest(@NotBlank String name) {
}
