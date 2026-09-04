package com.ramy.bugreport.controller;

import java.util.Map;
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

import com.ramy.bugreport.dto.component.CreateComponentRequest;
import com.ramy.bugreport.dto.component.CreateComponentResponse;
import com.ramy.bugreport.dto.component.UpdateComponentDescriptionRequest;
import com.ramy.bugreport.dto.component.UpdateComponentNameRequest;
import com.ramy.bugreport.dto.component.UpdateComponentResponsibleUserRequest;
import com.ramy.bugreport.dto.component.UpdateComponentResponse;
import com.ramy.bugreport.service.ComponentService;

import jakarta.validation.Valid;

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

    @GetMapping
    public Map<UUID, String> getAll() {
        return componentService.getAll();
    }

    /*
    * ============================================
    *
    * POST Mappings
    *
    * ============================================
    */

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

    @PatchMapping("/{componentId}/name")
    public ResponseEntity<UpdateComponentResponse> updateName(
            @PathVariable UUID componentId,
            @Valid @RequestBody UpdateComponentNameRequest request
    ) {
        return updateResponse(componentService.updateName(componentId, request));
    }

    @PatchMapping("/{componentId}/description")
    public ResponseEntity<UpdateComponentResponse> updateDescription(
            @PathVariable UUID componentId,
            @Valid @RequestBody UpdateComponentDescriptionRequest request
    ) {
        return updateResponse(componentService.updateDescription(componentId, request));
    }

    @PatchMapping("/{componentId}/responsibleUserId")
    public ResponseEntity<UpdateComponentResponse> updateResponsibleUserId(
            @PathVariable UUID componentId,
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
