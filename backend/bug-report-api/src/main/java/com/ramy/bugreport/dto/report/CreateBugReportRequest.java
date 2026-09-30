package com.ramy.bugreport.dto.report;

import java.util.UUID;

import com.ramy.bugreport.domain.EBugSeverity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

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
@Schema(description = "Request body of `POST /api/reports`.")
public record CreateBugReportRequest(
        @Schema(description = "Id of a developer to assign.", example = ApiExamples.UUID)
        UUID assigneeId,
        @Schema(description = "Id of the project.", example = ApiExamples.UUID)
        @NotNull UUID projectId,
        @Schema(description = "Id of the component.", example = ApiExamples.UUID)
        @NotNull UUID componentId,
        @Schema(description = "Short summary.", example = "Valid users cannot sign in")
        @NotBlank String title,
        @Schema(description = "Optional description.", example = "The sign-in endpoint returns HTTP 500 for valid credentials.")
        String description,
        @Schema(description = "Optional steps to reproduce.", example = "Open sign in, enter valid credentials, and submit the form.")
        String stepsToReproduce,
        @Schema(description = "Optional expected behavior.", example = "The user is authenticated and redirected to the dashboard.")
        String expectedBehavior,
        @Schema(description = "Optional actual behavior.", example = "The server returns HTTP 500 and the user remains signed out.")
        String actualBehavior,
        @Schema(description = "Severity.", example = "CRITICAL")
        @NotNull EBugSeverity severity
) {

}
