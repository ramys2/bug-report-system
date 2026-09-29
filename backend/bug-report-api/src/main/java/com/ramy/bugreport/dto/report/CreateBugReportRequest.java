package com.ramy.bugreport.dto.report;

import java.util.UUID;

import com.ramy.bugreport.domain.EBugSeverity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body of {@code POST /api/reports}.
 *
 * @param assigneeId id of a developer to assign; optional
 * @param projectId id of the project; required
 * @param componentId id of the component; required
 * @param title short summary; required, not blank
 * @param description optional description
 * @param stepsToReproduce optional steps to reproduce
 * @param expectedBehavior optional expected behavior
 * @param actualBehavior optional actual behavior
 * @param severity severity; required
 */
public record CreateBugReportRequest(
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
