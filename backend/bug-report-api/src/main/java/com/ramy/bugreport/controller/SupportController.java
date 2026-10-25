package com.ramy.bugreport.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ramy.bugreport.dto.support.CreateSupportRequestRequest;
import com.ramy.bugreport.dto.support.CreateSupportRequestResponse;
import com.ramy.bugreport.dto.support.SupportRequestResponse;
import com.ramy.bugreport.dto.support.UpdateSupportRequestStatusRequest;
import com.ramy.bugreport.dto.support.UpdateSupportRequestStatusResponse;
import com.ramy.bugreport.openapi.BadRequestResponse;
import com.ramy.bugreport.openapi.ConflictResponse;
import com.ramy.bugreport.openapi.ForbiddenResponse;
import com.ramy.bugreport.openapi.UnauthorizedResponse;
import com.ramy.bugreport.openapi.NotFoundResponse;
import com.ramy.bugreport.service.SupportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Support creation and administrator management. Identity and timestamps are always server supplied. */
@Tag(name = "Support")
@RestController
@RequestMapping("/api/support")
public class SupportController {
    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @Operation(summary = "List support requests", description = "Access: ADMIN only. Returns requests newest first, with requester id/name, details, status and timestamps.")
    @ApiResponse(responseCode = "200", description = "Support requests, newest first.")
    @UnauthorizedResponse
    @ForbiddenResponse
    @GetMapping
    public List<SupportRequestResponse> getAll() {
        return supportService.getAll();
    }

    /** Returns 201 for a saved request, 400 for invalid fields, or 409 for a business/uniqueness conflict. */
    @Operation(summary = "Create a support request", description = "Access: authenticated Reporter or Developer; CSRF token required. ROLE_CHANGE allows only Reporter to Developer. OTHER requires a non-blank description (up to 10000 characters) and no requestedRole. Identity and PENDING status are supplied by the server.")
    @ApiResponse(responseCode = "201", description = "Support request created. Returns code SUPPORT_REQUEST_CREATED and a confirmation message.")
    @BadRequestResponse
    @UnauthorizedResponse
    @ForbiddenResponse
    @ConflictResponse
    @PostMapping
    public ResponseEntity<CreateSupportRequestResponse> create(@Valid @RequestBody CreateSupportRequestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supportService.create(request));
    }

    @Operation(summary = "Resolve a support request", description = "Access: ADMIN only; CSRF token required. Supply APPROVED or REJECTED. Approval of a role change revalidates the current account and changes its role atomically with resolution. Self-approval is forbidden. Resolved/outdated requests return 409; outdated requests remain pending.")
    @ApiResponse(responseCode = "200", description = "Returns code SUPPORT_REQUEST_RESOLVED and a confirmation message.")
    @BadRequestResponse
    @UnauthorizedResponse
    @ForbiddenResponse
    @NotFoundResponse
    @ConflictResponse
    @PatchMapping("/{requestId}/status")
    public UpdateSupportRequestStatusResponse updateStatus(@PathVariable UUID requestId,
            @Valid @RequestBody UpdateSupportRequestStatusRequest request) {
        return supportService.updateStatus(requestId, request);
    }
}
