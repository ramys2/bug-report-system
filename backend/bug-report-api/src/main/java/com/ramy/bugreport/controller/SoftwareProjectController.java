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

/**
 * REST endpoints for software projects, under {@code /api/projects}.
 *
 * <p>Errors are returned as JSON {@code {"message": "..."}} ({@link com.ramy.bugreport.exception.ApiErrorResponse}):
 * 400 for invalid input, 401 when not signed in, 403 when the role or ownership check fails,
 * 404 when a referenced resource does not exist, 409 for business rule conflicts. Requests that
 * change data ({@code POST}, {@code PATCH}, {@code DELETE}) also need a CSRF token, see {@code GET /api/csrf}.
 */
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
     * @throws ResourceNotFoundException 404 if the project does not exist
     */
    @PatchMapping("/{projectId}/name")
    public ResponseEntity<UpdateSoftwareProjectResponse> updateName(
            @PathVariable UUID projectId,
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
     * @throws ResourceNotFoundException 404 if the project does not exist
     */
    @PatchMapping("/{projectId}/description")
    public ResponseEntity<UpdateSoftwareProjectResponse> updateDescription(
            @PathVariable UUID projectId,
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
