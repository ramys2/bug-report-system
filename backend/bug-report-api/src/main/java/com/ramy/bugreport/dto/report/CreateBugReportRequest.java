package com.ramy.bugreport.dto.report;

import java.util.UUID;

import com.ramy.bugreport.domain.EBugSeverity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBugReportRequest(
    @NotNull UUID reporterId,
    UUID assigneeId,
    @NotNull UUID projectId,
    @NotNull UUID componentId,
    @NotBlank String title,
    String description,
    String stepsToReproduce,
    String expectedBehavior,
    String actualBehavior,
    @NotNull EBugSeverity severity
) {

}
