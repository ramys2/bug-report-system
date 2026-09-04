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

    @PatchMapping("/{projectId}/name")
    public ResponseEntity<UpdateSoftwareProjectResponse> updateName(
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateSoftwareProjectNameRequest request
    ) {
        return updateResponse(softwareProjectService.updateName(projectId, request));
    }

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
