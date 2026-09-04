package com.ramy.bugreport.service;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ramy.bugreport.domain.Component;
import com.ramy.bugreport.dto.component.CreateComponentRequest;
import com.ramy.bugreport.dto.component.CreateComponentResponse;
import com.ramy.bugreport.dto.component.UpdateComponentDescriptionRequest;
import com.ramy.bugreport.dto.component.UpdateComponentNameRequest;
import com.ramy.bugreport.dto.component.UpdateComponentResponsibleUserRequest;
import com.ramy.bugreport.dto.component.UpdateComponentResponse;
import com.ramy.bugreport.exception.ResourceNotFoundException;
import com.ramy.bugreport.repository.IComponentRepository;

import jakarta.transaction.Transactional;

@Service
public class ComponentService {
    private final IComponentRepository componentRepository;

    public ComponentService(IComponentRepository componentRepository) {
        this.componentRepository = componentRepository;
    }

    /*
    * ============================================
    *
    * GET
    *
    * ============================================
    */

    public Map<UUID, String> getAll() {
        return componentRepository.findAll()
                .stream()
                .collect(Collectors.toMap(Component::getId, Component::getName));
    }

    /*
    * ============================================
    *
    * POST
    *
    * ============================================
    */

    @Transactional
    public CreateComponentResponse create(CreateComponentRequest request) {
        Component component = new Component(
                request.name(),
                request.description(),
                request.responsibleUserId());
        component = componentRepository.save(component);

        return new CreateComponentResponse(component.getId(), "Successfully created!");
    }

    /*
    * ============================================
    *
    * PATCH
    *
    * ============================================
    */

    @Transactional
    public UpdateComponentResponse updateName(UUID componentId, UpdateComponentNameRequest request) {
        var component = componentById(componentId);
        component.setName(request.name());
        return updateResponse(component);
    }

    @Transactional
    public UpdateComponentResponse updateDescription(
            UUID componentId,
            UpdateComponentDescriptionRequest request
    ) {
        var component = componentById(componentId);
        component.setDescription(request.description());
        return updateResponse(component);
    }

    @Transactional
    public UpdateComponentResponse updateResponsibleUserId(
            UUID componentId,
            UpdateComponentResponsibleUserRequest request
    ) {
        var component = componentById(componentId);
        component.setResponsibleUserId(request.responsibleUserId());
        return updateResponse(component);
    }

    private Component componentById(UUID componentId) {
        return componentRepository.findById(componentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Component with id=%s does not exist!".formatted(componentId)));
    }

    private UpdateComponentResponse updateResponse(Component component) {
        return new UpdateComponentResponse(component.getId(), "Component updated successfully!");
    }
}
