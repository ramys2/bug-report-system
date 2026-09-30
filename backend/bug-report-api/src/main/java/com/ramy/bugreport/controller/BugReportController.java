package com.ramy.bugreport.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ramy.bugreport.dto.report.BugReportBriefResponse;
import com.ramy.bugreport.dto.report.BugReportResponse;
import com.ramy.bugreport.dto.report.CloseBugReportRequest;
import com.ramy.bugreport.dto.report.CloseBugReportResponse;
import com.ramy.bugreport.dto.report.CreateBugReportRequest;
import com.ramy.bugreport.dto.report.CreateBugReportResponse;
import com.ramy.bugreport.dto.report.UpdateActualBehaviorRequest;
import com.ramy.bugreport.dto.report.UpdateAssigneeRequest;
import com.ramy.bugreport.dto.report.UpdateBugReportResponse;
import com.ramy.bugreport.dto.report.UpdateComponentRequest;
import com.ramy.bugreport.dto.report.UpdateDescriptionRequest;
import com.ramy.bugreport.dto.report.UpdateExpectedBehaviorRequest;
import com.ramy.bugreport.dto.report.UpdateProjectRequest;
import com.ramy.bugreport.dto.report.UpdateSeverityRequest;
import com.ramy.bugreport.dto.report.UpdateStatusRequest;
import com.ramy.bugreport.dto.report.UpdateStepsToReproduceRequest;
import com.ramy.bugreport.security.UserAccountDetails;
import com.ramy.bugreport.service.BugReportService;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.ramy.bugreport.openapi.ApiExamples;
import com.ramy.bugreport.openapi.BadRequestResponse;
import com.ramy.bugreport.openapi.UnauthorizedResponse;
import com.ramy.bugreport.openapi.ForbiddenResponse;
import com.ramy.bugreport.openapi.NotFoundResponse;
import com.ramy.bugreport.openapi.ConflictResponse;



/**
 * REST endpoints for bug reports, under {@code /api/reports}.
 *
 * <p>Errors are returned as JSON {@code {"message": "..."}} ({@link com.ramy.bugreport.exception.ApiErrorResponse}):
 * 400 for invalid input, 401 when not signed in, 403 when the role or ownership check fails,
 * 404 when a referenced resource does not exist, 409 for business rule conflicts. Requests that
 * change data ({@code POST}, {@code PATCH}, {@code DELETE}) also need a CSRF token, see {@code GET /api/csrf}.
 *
 * <p>Bodies and responses use camelCase JSON. Update endpoints ({@code PATCH /{reportId}/...})
 * change one field each. Report fields are documented on {@link com.ramy.bugreport.domain.BugReport}.
 */
@Tag(name = "Bug reports")
@RestController
@RequestMapping("/api/reports")
public class BugReportController {
    private final BugReportService reportService;

    public BugReportController(BugReportService bugReportService) {
        reportService = bugReportService;
    }
    
    /*
    * ============================================
    *
    * GET Mappings
    *
    * ============================================
    */

    /**
     * {@code GET /api/reports}: lists all reports in brief form.
     *
     * <p>Access: any signed-in user. There is no filtering by owner.
     *
     * @return 200 with a list of {@code {reportId, title, author, assignee, status, severity, createdAt}};
     *         {@code assignee} is {@code null} if unassigned and {@code createdAt} is formatted {@code dd-MM-yyyy HH:mm}
     */
    @Operation(summary = "List all bug reports", description = "Lists all reports in brief form. Access: any signed-in user. There is no filtering by owner.")
    @ApiResponse(responseCode = "200", description = "List of reports in brief form.")
    @UnauthorizedResponse
    @GetMapping
    public List<BugReportBriefResponse> getAll() {
        return reportService.getAll();
    }
    
    /**
     * {@code GET /api/reports/{reportId}}: returns the full detail of one report.
     *
     * <p>Access: any signed-in user.
     *
     * @param reportId id of the report
     * @return 200 with {@code {id, reporterName, assigneeName, projectName, componentName, title, description,
     *         stepsToReproduce, expectedBehavior, actualBehavior, severity, status, createdAt, updatedAt, resolution}};
     *         {@code resolution} is {@code null} until the report is closed, otherwise
     *         {@code {id, description, resolvedAt, fixedVersion, commitUrl}}
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if the report or a user, project or component it refers to does not exist
     */
    @Operation(summary = "Get a bug report", description = "Returns the full detail of one report. `resolution` is null until the report is closed. Access: any signed-in user.")
    @ApiResponse(responseCode = "200", description = "The report with its resolution, if any.")
    @BadRequestResponse
    @UnauthorizedResponse
    @NotFoundResponse
    @GetMapping("/{reportId}")
    public BugReportResponse getReport(
            @Parameter(description = "Id of the bug report.", example = ApiExamples.UUID) @PathVariable UUID reportId
    ) {
        return reportService.getReport(reportId);
    }

    /**
     * {@code GET /api/reports/reported}: lists the reports filed by the signed-in user.
     *
     * <p>Access: any signed-in user.
     *
     * @return 200 with a list in the same brief form as {@link #getAll()}; empty if there are none
     */
    @Operation(summary = "List reports filed by the signed-in user", description = "Access: any signed-in user. The list is empty if there are none.")
    @ApiResponse(responseCode = "200", description = "Reports filed by the signed-in user.")
    @UnauthorizedResponse
    @GetMapping("/reported")
    public List<BugReportBriefResponse> getReported(
    		@AuthenticationPrincipal UserAccountDetails account
    ) {
        return reportService.getReportsByReporter(account.getId());
    }
    
    /**
     * {@code GET /api/reports/assigned}: lists the reports assigned to the signed-in user, including closed ones.
     *
     * <p>Access: any signed-in user.
     *
     * @return 200 with a list in the same brief form as {@link #getAll()}; empty if there are none
     */
    @Operation(summary = "List reports assigned to the signed-in user", description = "Includes closed reports. Access: any signed-in user. The list is empty if there are none.")
    @ApiResponse(responseCode = "200", description = "Reports assigned to the signed-in user.")
    @UnauthorizedResponse
    @GetMapping("/assigned")
    public List<BugReportBriefResponse> getAssigned(
    		@AuthenticationPrincipal UserAccountDetails account
    ) {
        return reportService.getReportsByAssignee(account.getId());
    }
    
    /*
    * ============================================
    *
    * POST Mappings
    *
    * ============================================
    */

    /**
     * {@code POST /api/reports}: files a new report as the signed-in user. The report starts with status {@code OPEN}.
     *
     * <p>Access: any signed-in user.
     *
     * @param request body {@code {projectId, componentId, title, severity}} are required (title must not be blank);
     *         {@code assigneeId, description, stepsToReproduce, expectedBehavior, actualBehavior} are optional
     * @return 201 with {@code {id, message}}
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if the project or component does not exist
     * @throws com.ramy.bugreport.exception.BusinessRuleConflictException 409 if {@code assigneeId} is not a developer
     */
    @Operation(summary = "Create a bug report", description = "Files a new report as the signed-in user. The report starts with status `OPEN`. Returns 404 if the project or component does not exist and 409 if `assigneeId` is not a developer. Access: any signed-in user.")
    @ApiResponse(responseCode = "201", description = "Report created.")
    @BadRequestResponse
    @UnauthorizedResponse
    @ForbiddenResponse
    @NotFoundResponse
    @ConflictResponse
    @PostMapping
    public ResponseEntity<CreateBugReportResponse> create(
            @Valid @RequestBody CreateBugReportRequest request,
            @AuthenticationPrincipal UserAccountDetails account
    ) {
        var response = reportService.create(account.getId(), request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }
    
    /**
     * {@code POST /api/reports/{reportId}/resolution}: closes a report by creating its resolution and setting its status to {@code CLOSED}.
     * The reporter and assignee are notified by email.
     *
     * <p>Access: signed-in user; the service further requires the ADMIN role, or being the report's reporter or assignee.
     *
     * @param reportId id of the report
     * @param request body {@code {description}} is required and not blank; {@code fixedVersion, commitUrl} are optional
     * @return 201 with {@code {reportId, message}}
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if the report does not exist
     * @throws com.ramy.bugreport.exception.BusinessRuleConflictException 409 if the report is already closed
     */
    @Operation(summary = "Close a bug report with a resolution", description = "Creates the resolution and sets the status to `CLOSED`; the reporter and assignee are notified by email. Returns 409 if the report is already closed. Access: signed-in user; the service further requires the ADMIN role, or being the report's reporter or assignee.")
    @ApiResponse(responseCode = "201", description = "Resolution created and report closed.")
    @BadRequestResponse
    @UnauthorizedResponse
    @ForbiddenResponse
    @NotFoundResponse
    @ConflictResponse
    @PostMapping("/{reportId}/resolution")
    public ResponseEntity<CloseBugReportResponse> close(
            @Parameter(description = "Id of the bug report.", example = ApiExamples.UUID) @PathVariable UUID reportId,
            @Valid @RequestBody CloseBugReportRequest request
    ) {
        var response = reportService.close(reportId, request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    
    /*
    * ============================================
    *
    * PATCH Mappings
    *
    * ============================================
    */
    
    /**
     * {@code PATCH /api/reports/{reportId}/assignee}: Assigns the report to a developer; the developer is notified by email.
     *
     * <p>Access: signed-in user; the service further requires the ADMIN role, or being the report's reporter or assignee.
     *
     * @param reportId id of the report
     * @param request body {@code assigneeId}; required, must be the id of a user with the DEVELOPER role
     * @return 200 with the report id and a confirmation message
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if the report, the assignee or the reporter does not exist
     * @throws com.ramy.bugreport.exception.BusinessRuleConflictException 409 if the report is closed or the assignee is not a developer
     */
    @Operation(summary = "Change the assignee", description = "Assigns the report to a developer; the developer is notified by email. Returns 409 if the report is closed or the user is not a developer. Access: signed-in user; the service further requires the ADMIN role, or being the report's reporter or assignee.")
    @ApiResponse(responseCode = "200", description = "Assignee changed.")
    @BadRequestResponse
    @UnauthorizedResponse
    @ForbiddenResponse
    @NotFoundResponse
    @ConflictResponse
    @PatchMapping("/{reportId}/assignee")
    public ResponseEntity<UpdateBugReportResponse> updateAssignee(
            @Parameter(description = "Id of the bug report.", example = ApiExamples.UUID) @PathVariable UUID reportId,
            @Valid @RequestBody UpdateAssigneeRequest request
    ) {
        return updateResponse(reportService.updateAssignee(reportId, request));
    }

    /**
     * {@code PATCH /api/reports/{reportId}/severity}: Changes the severity.
     *
     * <p>Access: signed-in user; the service further requires the ADMIN role, or being the report's reporter or assignee.
     *
     * @param reportId id of the report
     * @param request body {@code severity}; required, one of LOW, MEDIUM, HIGH, CRITICAL
     * @return 200 with the report id and a confirmation message
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if the report does not exist
     * @throws com.ramy.bugreport.exception.BusinessRuleConflictException 409 if the report is closed
     */
    @Operation(summary = "Change the severity", description = "Returns 409 if the report is closed. Access: signed-in user; the service further requires the ADMIN role, or being the report's reporter or assignee.")
    @ApiResponse(responseCode = "200", description = "Severity changed.")
    @BadRequestResponse
    @UnauthorizedResponse
    @ForbiddenResponse
    @NotFoundResponse
    @ConflictResponse
    @PatchMapping("/{reportId}/severity")
    public ResponseEntity<UpdateBugReportResponse> updateSeverity(
            @Parameter(description = "Id of the bug report.", example = ApiExamples.UUID) @PathVariable UUID reportId,
            @Valid @RequestBody UpdateSeverityRequest request
    ) {
        return updateResponse(reportService.updateSeverity(reportId, request));
    }

    /**
     * {@code PATCH /api/reports/{reportId}/status}: Changes the status; the reporter and assignee are notified by email. Use the resolution endpoint to close a report.
     *
     * <p>Access: signed-in user; the service further requires the ADMIN role, or being the report's reporter or assignee.
     *
     * @param reportId id of the report
     * @param request body {@code status}; required, one of OPEN, ASSIGNED, IN_PROGRESS, NEEDS_INFORMATION, REVIEWING, REJECTED, CLOSED
     * @return 200 with the report id and a confirmation message
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if the report does not exist
     * @throws com.ramy.bugreport.exception.BusinessRuleConflictException 409 if the report is closed, or the requested status is {@code CLOSED}
     */
    @Operation(summary = "Change the status", description = "The reporter and assignee are notified by email. Use the resolution endpoint to close a report. Returns 409 if the report is closed or the requested status is `CLOSED`. Access: signed-in user; the service further requires the ADMIN role, or being the report's reporter or assignee.")
    @ApiResponse(responseCode = "200", description = "Status changed.")
    @BadRequestResponse
    @UnauthorizedResponse
    @ForbiddenResponse
    @NotFoundResponse
    @ConflictResponse
    @PatchMapping("/{reportId}/status")
    public ResponseEntity<UpdateBugReportResponse> updateStatus(
            @Parameter(description = "Id of the bug report.", example = ApiExamples.UUID) @PathVariable UUID reportId,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        return updateResponse(reportService.updateStatus(reportId, request));
    }

    /**
     * {@code PATCH /api/reports/{reportId}/project}: Moves the report to another project.
     *
     * <p>Access: signed-in user; the service further requires the ADMIN role, or being the report's reporter or assignee.
     *
     * @param reportId id of the report
     * @param request body {@code projectId}; required
     * @return 200 with the report id and a confirmation message
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if the report, or the project, does not exist
     * @throws com.ramy.bugreport.exception.BusinessRuleConflictException 409 if the report is closed
     */
    @Operation(summary = "Move the report to another project", description = "Returns 409 if the report is closed. Access: signed-in user; the service further requires the ADMIN role, or being the report's reporter or assignee.")
    @ApiResponse(responseCode = "200", description = "Project changed.")
    @BadRequestResponse
    @UnauthorizedResponse
    @ForbiddenResponse
    @NotFoundResponse
    @ConflictResponse
    @PatchMapping("/{reportId}/project")
    public ResponseEntity<UpdateBugReportResponse> updateProject(
            @Parameter(description = "Id of the bug report.", example = ApiExamples.UUID) @PathVariable UUID reportId,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        return updateResponse(reportService.updateProject(reportId, request));
    }

    /**
     * {@code PATCH /api/reports/{reportId}/component}: Moves the report to another component (not checked against the report's project).
     *
     * <p>Access: signed-in user; the service further requires the ADMIN role, or being the report's reporter or assignee.
     *
     * @param reportId id of the report
     * @param request body {@code componentId}; required
     * @return 200 with the report id and a confirmation message
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if the report, or the component, does not exist
     * @throws com.ramy.bugreport.exception.BusinessRuleConflictException 409 if the report is closed
     */
    @Operation(summary = "Move the report to another component", description = "The component is not checked against the report's project. Returns 409 if the report is closed. Access: signed-in user; the service further requires the ADMIN role, or being the report's reporter or assignee.")
    @ApiResponse(responseCode = "200", description = "Component changed.")
    @BadRequestResponse
    @UnauthorizedResponse
    @ForbiddenResponse
    @NotFoundResponse
    @ConflictResponse
    @PatchMapping("/{reportId}/component")
    public ResponseEntity<UpdateBugReportResponse> updateComponent(
            @Parameter(description = "Id of the bug report.", example = ApiExamples.UUID) @PathVariable UUID reportId,
            @Valid @RequestBody UpdateComponentRequest request
    ) {
        return updateResponse(reportService.updateComponent(reportId, request));
    }

    /**
     * {@code PATCH /api/reports/{reportId}/description}: Replaces the description.
     *
     * <p>Access: signed-in user; the service further requires the ADMIN role, or being the report's reporter or assignee.
     *
     * @param reportId id of the report
     * @param request body {@code description}; required, may be empty but not null
     * @return 200 with the report id and a confirmation message
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if the report does not exist
     * @throws com.ramy.bugreport.exception.BusinessRuleConflictException 409 if the report is closed
     */
    @Operation(summary = "Replace the description", description = "Returns 409 if the report is closed. Access: signed-in user; the service further requires the ADMIN role, or being the report's reporter or assignee.")
    @ApiResponse(responseCode = "200", description = "Description replaced.")
    @BadRequestResponse
    @UnauthorizedResponse
    @ForbiddenResponse
    @NotFoundResponse
    @ConflictResponse
    @PatchMapping("/{reportId}/description")
    public ResponseEntity<UpdateBugReportResponse> updateDescription(
            @Parameter(description = "Id of the bug report.", example = ApiExamples.UUID) @PathVariable UUID reportId,
            @Valid @RequestBody UpdateDescriptionRequest request
    ) {
        return updateResponse(reportService.updateDescription(reportId, request));
    }

    /**
     * {@code PATCH /api/reports/{reportId}/steps-to-reproduce}: Replaces the steps to reproduce.
     *
     * <p>Access: signed-in user; the service further requires the ADMIN role, or being the report's reporter or assignee.
     *
     * @param reportId id of the report
     * @param request body {@code stepsToReproduce}; required, may be empty but not null
     * @return 200 with the report id and a confirmation message
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if the report does not exist
     * @throws com.ramy.bugreport.exception.BusinessRuleConflictException 409 if the report is closed
     */
    @Operation(summary = "Replace the steps to reproduce", description = "Returns 409 if the report is closed. Access: signed-in user; the service further requires the ADMIN role, or being the report's reporter or assignee.")
    @ApiResponse(responseCode = "200", description = "Steps to reproduce replaced.")
    @BadRequestResponse
    @UnauthorizedResponse
    @ForbiddenResponse
    @NotFoundResponse
    @ConflictResponse
    @PatchMapping("/{reportId}/steps-to-reproduce")
    public ResponseEntity<UpdateBugReportResponse> updateStepsToReproduce(
            @Parameter(description = "Id of the bug report.", example = ApiExamples.UUID) @PathVariable UUID reportId,
            @Valid @RequestBody UpdateStepsToReproduceRequest request
    ) {
        return updateResponse(reportService.updateStepsToReproduce(reportId, request));
    }

    /**
     * {@code PATCH /api/reports/{reportId}/expected-behavior}: Replaces the expected behavior.
     *
     * <p>Access: signed-in user; the service further requires the ADMIN role, or being the report's reporter or assignee.
     *
     * @param reportId id of the report
     * @param request body {@code expectedBehavior}; required, may be empty but not null
     * @return 200 with the report id and a confirmation message
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if the report does not exist
     * @throws com.ramy.bugreport.exception.BusinessRuleConflictException 409 if the report is closed
     */
    @Operation(summary = "Replace the expected behavior", description = "Returns 409 if the report is closed. Access: signed-in user; the service further requires the ADMIN role, or being the report's reporter or assignee.")
    @ApiResponse(responseCode = "200", description = "Expected behavior replaced.")
    @BadRequestResponse
    @UnauthorizedResponse
    @ForbiddenResponse
    @NotFoundResponse
    @ConflictResponse
    @PatchMapping("/{reportId}/expected-behavior")
    public ResponseEntity<UpdateBugReportResponse> updateExpectedBehavior(
            @Parameter(description = "Id of the bug report.", example = ApiExamples.UUID) @PathVariable UUID reportId,
            @Valid @RequestBody UpdateExpectedBehaviorRequest request
    ) {
        return updateResponse(reportService.updateExpectedBehavior(reportId, request));
    }

    /**
     * {@code PATCH /api/reports/{reportId}/actual-behavior}: Replaces the actual behavior.
     *
     * <p>Access: signed-in user; the service further requires the ADMIN role, or being the report's reporter or assignee.
     *
     * @param reportId id of the report
     * @param request body {@code actualBehavior}; required, may be empty but not null
     * @return 200 with the report id and a confirmation message
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if the report does not exist
     * @throws com.ramy.bugreport.exception.BusinessRuleConflictException 409 if the report is closed
     */
    @Operation(summary = "Replace the actual behavior", description = "Returns 409 if the report is closed. Access: signed-in user; the service further requires the ADMIN role, or being the report's reporter or assignee.")
    @ApiResponse(responseCode = "200", description = "Actual behavior replaced.")
    @BadRequestResponse
    @UnauthorizedResponse
    @ForbiddenResponse
    @NotFoundResponse
    @ConflictResponse
    @PatchMapping("/{reportId}/actual-behavior")
    public ResponseEntity<UpdateBugReportResponse> updateActualBehavior(
            @Parameter(description = "Id of the bug report.", example = ApiExamples.UUID) @PathVariable UUID reportId,
            @Valid @RequestBody UpdateActualBehaviorRequest request
    ) {
        return updateResponse(reportService.updateActualBehavior(reportId, request));
    }

    private ResponseEntity<UpdateBugReportResponse> updateResponse(
            UpdateBugReportResponse response
    ) {
        return ResponseEntity.ok(response);
    }
    
}
