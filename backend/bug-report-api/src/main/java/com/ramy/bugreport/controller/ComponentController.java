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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ramy.bugreport.dto.component.CreateComponentRequest;
import com.ramy.bugreport.dto.component.CreateComponentResponse;
import com.ramy.bugreport.dto.component.ComponentResponse;
import com.ramy.bugreport.dto.component.UpdateComponentDescriptionRequest;
import com.ramy.bugreport.dto.component.UpdateComponentNameRequest;
import com.ramy.bugreport.dto.component.UpdateComponentResponsibleUserRequest;
import com.ramy.bugreport.dto.component.UpdateComponentResponse;
import com.ramy.bugreport.service.ComponentService;

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
import com.ramy.bugreport.openapi.ConflictResponse;

/**
 * REST endpoints for components, under {@code /api/components}.
 *
 * <p>Errors are returned as JSON {@code {"message": "..."}} ({@link com.ramy.bugreport.exception.ApiErrorResponse}):
 * 400 for invalid input, 401 when not signed in, 403 when the role or ownership check fails,
 * 404 when a referenced resource does not exist, 409 for business rule conflicts. Requests that
 * change data ({@code POST}, {@code PATCH}, {@code DELETE}) also need a CSRF token, see {@code GET /api/csrf}.
 */
@Tag(name = "Components")
@RestController
@RequestMapping("/api/components")
public class ComponentController {
    private final ComponentService componentService;

    public ComponentController(ComponentService componentService) {
        this.componentService = componentService;
    }

    /*
    * ============================================
    *
    * GET Mappings
    *
    * ============================================
    */

    /**
     * {@code GET /api/components}: lists all components, or only the components of one project.
     *
     * <p>Access: any signed-in user.
     *
     * @param projectId optional; if given, only components of this project are listed
     * @return 200 with a list of {@code {id, name, description, responsibleUserName, projectId}}
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if {@code projectId} is given but no such project exists
     */
    @Operation(summary = "List components", description = "Access: any signed-in user. Without `projectId` all components are returned; with it only the components of that project, or 404 if the project does not exist.")
    @ApiResponse(responseCode = "200", description = "The components.")
    @UnauthorizedResponse
    @NotFoundResponse
    @GetMapping
    public List<ComponentResponse> getAll(
            @Parameter(description = "Only list components of this project.", example = ApiExamples.UUID) @RequestParam(required = false) UUID projectId
    ) {
        return componentService.getAll(projectId);
    }

    /*
    * ============================================
    *
    * POST Mappings
    *
    * ============================================
    */

    /**
     * {@code POST /api/components}: creates a component.
     *
     * <p>Access: ADMIN or DEVELOPER role.
     *
     * @param request body {@code {name, projectId}} are required ({@code name} not blank); {@code description} and {@code responsibleUserId} are optional
     * @return 201 with {@code {id, message}}
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if the project does not exist, or {@code responsibleUserId} is given but no such user exists
     */
    @Operation(summary = "Create a component", description = "Access: ADMIN or DEVELOPER role. `projectId` must belong to an existing project. `responsibleUserId` is optional; if given, it must belong to an existing user. Otherwise the request fails with 404.")
    @ApiResponse(responseCode = "201", description = "Component created.")
    @BadRequestResponse
    @UnauthorizedResponse
    @ForbiddenResponse
    @NotFoundResponse
    @PostMapping
    public ResponseEntity<CreateComponentResponse> create(
            @Valid @RequestBody CreateComponentRequest request
    ) {
        var response = componentService.create(request);
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
     * {@code PATCH /api/components/{componentId}/name}: Renames a component.
     *
     * <p>Access: ADMIN or DEVELOPER role.
     *
     * @param componentId id of the component
     * @param request body {@code name}; required, not blank
     * @return 200 with {@code {id, message}}
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if the component does not exist
     */
    @Operation(summary = "Rename a component", description = "Access: ADMIN or DEVELOPER role.")
    @ApiResponse(responseCode = "200", description = "Component renamed.")
    @BadRequestResponse
    @UnauthorizedResponse
    @ForbiddenResponse
    @NotFoundResponse
    @PatchMapping("/{componentId}/name")
    public ResponseEntity<UpdateComponentResponse> updateName(
            @Parameter(description = "Id of the component.", example = ApiExamples.UUID) @PathVariable UUID componentId,
            @Valid @RequestBody UpdateComponentNameRequest request
    ) {
        return updateResponse(componentService.updateName(componentId, request));
    }

    /**
     * {@code PATCH /api/components/{componentId}/description}: Replaces a component's description.
     *
     * <p>Access: ADMIN or DEVELOPER role.
     *
     * @param componentId id of the component
     * @param request body {@code description}; required, may be empty but not null
     * @return 200 with {@code {id, message}}
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if the component does not exist
     */
    @Operation(summary = "Replace a component's description", description = "Access: ADMIN or DEVELOPER role.")
    @ApiResponse(responseCode = "200", description = "Description replaced.")
    @BadRequestResponse
    @UnauthorizedResponse
    @ForbiddenResponse
    @NotFoundResponse
    @PatchMapping("/{componentId}/description")
    public ResponseEntity<UpdateComponentResponse> updateDescription(
            @Parameter(description = "Id of the component.", example = ApiExamples.UUID) @PathVariable UUID componentId,
            @Valid @RequestBody UpdateComponentDescriptionRequest request
    ) {
        return updateResponse(componentService.updateDescription(componentId, request));
    }

    /**
     * {@code PATCH /api/components/{componentId}/responsibleUserId}: Changes the responsible user (note the camelCase path segment).
     *
     * <p>Access: ADMIN or DEVELOPER role.
     *
     * @param componentId id of the component
     * @param request body {@code responsibleUserId}; required
     * @return 200 with {@code {id, message}}
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if the component or the user does not exist
     */
    @Operation(summary = "Change the responsible user", description = "Access: ADMIN or DEVELOPER role. Note the camelCase path segment. Fails with 404 if the component or the user does not exist.")
    @ApiResponse(responseCode = "200", description = "Responsible user changed.")
    @BadRequestResponse
    @UnauthorizedResponse
    @ForbiddenResponse
    @NotFoundResponse
    @ConflictResponse
    @PatchMapping("/{componentId}/responsibleUserId")
    public ResponseEntity<UpdateComponentResponse> updateResponsibleUserId(
            @Parameter(description = "Id of the component.", example = ApiExamples.UUID) @PathVariable UUID componentId,
            @Valid @RequestBody UpdateComponentResponsibleUserRequest request
    ) {
        return updateResponse(componentService.updateResponsibleUserId(componentId, request));
    }

    private ResponseEntity<UpdateComponentResponse> updateResponse(
            UpdateComponentResponse response
    ) {
        return ResponseEntity.ok(response);
    }
}
