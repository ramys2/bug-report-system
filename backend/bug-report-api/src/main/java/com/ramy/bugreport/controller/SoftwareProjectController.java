package com.ramy.bugreport.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ramy.bugreport.dto.project.CreateSoftwareProjectRequest;
import com.ramy.bugreport.dto.project.CreateSoftwareProjectResponse;
import com.ramy.bugreport.dto.project.SoftwareProjectResponse;
import com.ramy.bugreport.dto.project.UpdateSoftwareProjectDescriptionRequest;
import com.ramy.bugreport.dto.project.UpdateSoftwareProjectNameRequest;
import com.ramy.bugreport.dto.project.UpdateSoftwareProjectResponse;
import com.ramy.bugreport.service.SoftwareProjectService;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.ramy.bugreport.openapi.ApiExamples;
import com.ramy.bugreport.openapi.BadRequestResponse;
import com.ramy.bugreport.openapi.UnauthorizedResponse;
import com.ramy.bugreport.openapi.ForbiddenResponse;
import com.ramy.bugreport.openapi.NotFoundResponse;

/**
 * REST endpoints for software projects, under {@code /api/projects}.
 *
 * <p>Errors are returned as JSON {@code {"message": "..."}} ({@link com.ramy.bugreport.exception.ApiErrorResponse}):
 * 400 for invalid input, 401 when not signed in, 403 when the role or ownership check fails,
 * 404 when a referenced resource does not exist, 409 for business rule conflicts. Requests that
 * change data ({@code POST}, {@code PATCH}, {@code DELETE}) also need a CSRF token, see {@code GET /api/csrf}.
 */
@Tag(name = "Projects")
@RestController
@RequestMapping("/api/projects")
public class SoftwareProjectController {
    private final SoftwareProjectService softwareProjectService;

    public SoftwareProjectController(SoftwareProjectService softwareProjectService) {
        this.softwareProjectService = softwareProjectService;
    }

    /*
    * ============================================
    *
    * GET Mappings
    *
    * ============================================
    */

    /**
     * {@code GET /api/projects}: lists all projects.
     *
     * <p>Access: any signed-in user.
     *
     * @return 200 with a list of {@code {id, name, description}}
     */
    @Operation(summary = "List all projects", description = "Access: any signed-in user.")
    @ApiResponse(responseCode = "200", description = "All projects.")
    @UnauthorizedResponse
    @GetMapping
    public List<SoftwareProjectResponse> getAll() {
        return softwareProjectService.getAll();
    }

    /*
    * ============================================
    *
    * POST Mappings
    *
    * ============================================
    */

    /**
     * {@code POST /api/projects}: creates a project.
     *
     * <p>Access: ADMIN role.
     *
     * @param request body {@code {name}} is required and not blank; {@code description} is optional
     * @return 201 with {@code {id, message}}
     */
    @Operation(summary = "Create a project", description = "Access: ADMIN role.")
    @ApiResponse(responseCode = "201", description = "Project created.")
    @BadRequestResponse
    @UnauthorizedResponse
    @ForbiddenResponse
    @PostMapping
    public ResponseEntity<CreateSoftwareProjectResponse> create(
            @Valid @RequestBody CreateSoftwareProjectRequest request
    ) {
        var response = softwareProjectService.create(request);
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
     * {@code PATCH /api/projects/{projectId}/name}: Renames a project.
     *
     * <p>Access: ADMIN or DEVELOPER role.
     *
     * @param projectId id of the project
     * @param request body {@code name}; required, not blank
     * @return 200 with {@code {id, message}}
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if the project does not exist
     */
    @Operation(summary = "Rename a project", description = "Access: ADMIN or DEVELOPER role.")
    @ApiResponse(responseCode = "200", description = "Project renamed.")
    @BadRequestResponse
    @UnauthorizedResponse
    @ForbiddenResponse
    @NotFoundResponse
    @PatchMapping("/{projectId}/name")
    public ResponseEntity<UpdateSoftwareProjectResponse> updateName(
            @Parameter(description = "Id of the project.", example = ApiExamples.UUID) @PathVariable UUID projectId,
            @Valid @RequestBody UpdateSoftwareProjectNameRequest request
    ) {
        return updateResponse(softwareProjectService.updateName(projectId, request));
    }

    /**
     * {@code PATCH /api/projects/{projectId}/description}: Replaces a project's description.
     *
     * <p>Access: ADMIN or DEVELOPER role.
     *
     * @param projectId id of the project
     * @param request body {@code description}; required, may be empty but not null
     * @return 200 with {@code {id, message}}
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if the project does not exist
     */
    @Operation(summary = "Replace a project's description", description = "Access: ADMIN or DEVELOPER role.")
    @ApiResponse(responseCode = "200", description = "Description replaced.")
    @BadRequestResponse
    @UnauthorizedResponse
    @ForbiddenResponse
    @NotFoundResponse
    @PatchMapping("/{projectId}/description")
    public ResponseEntity<UpdateSoftwareProjectResponse> updateDescription(
            @Parameter(description = "Id of the project.", example = ApiExamples.UUID) @PathVariable UUID projectId,
            @Valid @RequestBody UpdateSoftwareProjectDescriptionRequest request
    ) {
        return updateResponse(softwareProjectService.updateDescription(projectId, request));
    }

    private ResponseEntity<UpdateSoftwareProjectResponse> updateResponse(
            UpdateSoftwareProjectResponse response
    ) {
        return ResponseEntity.ok(response);
    }
}
