package com.ramy.bugreport.service;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ramy.bugreport.domain.Component;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.dto.component.CreateComponentRequest;
import com.ramy.bugreport.dto.component.CreateComponentResponse;
import com.ramy.bugreport.dto.component.ComponentResponse;
import com.ramy.bugreport.dto.component.UpdateComponentDescriptionRequest;
import com.ramy.bugreport.dto.component.UpdateComponentNameRequest;
import com.ramy.bugreport.dto.component.UpdateComponentResponsibleUserRequest;
import com.ramy.bugreport.dto.component.UpdateComponentResponse;
import com.ramy.bugreport.exception.ResourceNotFoundException;
import com.ramy.bugreport.repository.IComponentRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;

import jakarta.transaction.Transactional;

@Service
public class ComponentService {
    private final IComponentRepository componentRepository;
    private final IUserAccountRepository userAccountRepository;

    public ComponentService(
            IComponentRepository componentRepository,
            IUserAccountRepository userAccountRepository
    ) {
        this.componentRepository = componentRepository;
        this.userAccountRepository = userAccountRepository;
    }

    /*
    * ============================================
    *
    * GET
    *
    * ============================================
    */

    public List<ComponentResponse> getAll() {
        var components = componentRepository.findAll();
        var responsibleUserIds = components.stream()
                .map(Component::getResponsibleUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        var responsibleUsersById = userAccountRepository.findAllById(responsibleUserIds).stream()
                .collect(Collectors.toMap(UserAccount::getId, Function.identity()));

        return components.stream()
                .map(component -> ComponentResponse.from(
                        component,
                        responsibleUsersById.get(component.getResponsibleUserId())))
                .toList();
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
        componentRepository.save(component);
        return updateResponse(component);
    }

    @Transactional
    public UpdateComponentResponse updateDescription(
            UUID componentId,
            UpdateComponentDescriptionRequest request
    ) {
        var component = componentById(componentId);
        component.setDescription(request.description());
        componentRepository.save(component);
        return updateResponse(component);
    }

    @Transactional
    public UpdateComponentResponse updateResponsibleUserId(
            UUID componentId,
            UpdateComponentResponsibleUserRequest request
    ) {
        var component = componentById(componentId);
        component.setResponsibleUserId(request.responsibleUserId());
        componentRepository.save(component);
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
