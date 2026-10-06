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
import com.ramy.bugreport.repository.ISoftwareProjectRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;

import jakarta.transaction.Transactional;

/** Business logic for components (parts of a project that reports are filed against). */
@Service
public class ComponentService {
    private final IComponentRepository componentRepository;
    private final IUserAccountRepository userAccountRepository;
    private final ISoftwareProjectRepository softwareProjectRepository;

    public ComponentService(
            IComponentRepository componentRepository,
            IUserAccountRepository userAccountRepository,
            ISoftwareProjectRepository softwareProjectRepository
    ) {
        this.componentRepository = componentRepository;
        this.userAccountRepository = userAccountRepository;
        this.softwareProjectRepository = softwareProjectRepository;
    }

    /*
    * ============================================
    *
    * GET
    *
    * ============================================
    */

    /**
     * Returns the components together with their responsible user, which is loaded with one query for all components.
     *
     * @param projectId only components of this project are returned; {@code null} returns all components
     * @throws ResourceNotFoundException if a project id is given and no such project exists
     */
    public List<ComponentResponse> getAll(UUID projectId) {
        List<Component> components;
        if (projectId == null) {
            components = componentRepository.findAll();
        } else {
            requireProjectExists(projectId);
            components = componentRepository.findByProjectId(projectId);
        }
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

    /**
     * Creates a component.
     *
     * <p>The project must exist. The responsible user is optional; if an id is given, the user must exist.
     *
     * @return the id of the new component
     * @throws ResourceNotFoundException if the project does not exist, or a responsible user id is given and no such user exists
     */
    @Transactional
    public CreateComponentResponse create(CreateComponentRequest request) {
        requireProjectExists(request.projectId());
        if (request.responsibleUserId() != null) {
            requireUserExists(request.responsibleUserId());
        }
        Component component = new Component(
                request.name(),
                request.description(),
                request.responsibleUserId(),
                request.projectId());
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

    /**
     * Renames a component.
     *
     * @throws ResourceNotFoundException if the component does not exist
     */
    @Transactional
    public UpdateComponentResponse updateName(UUID componentId, UpdateComponentNameRequest request) {
        var component = componentById(componentId);
        component.setName(request.name());
        componentRepository.save(component);
        return updateResponse(component);
    }

    /**
     * Replaces a component's description.
     *
     * @throws ResourceNotFoundException if the component does not exist
     */
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

    /**
     * Changes the user responsible for a component.
     *
     * @throws ResourceNotFoundException if the component or the user does not exist
     */
    @Transactional
    public UpdateComponentResponse updateResponsibleUserId(
            UUID componentId,
            UpdateComponentResponsibleUserRequest request
    ) {
        var component = componentById(componentId);
        requireUserExists(request.responsibleUserId());
        component.setResponsibleUserId(request.responsibleUserId());
        componentRepository.save(component);
        return updateResponse(component);
    }

    private Component componentById(UUID componentId) {
        return componentRepository.findById(componentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Component with id=%s does not exist!".formatted(componentId)));
    }

    private void requireProjectExists(UUID projectId) {
        if (!softwareProjectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project with id=%s does not exist!".formatted(projectId));
        }
    }

    private void requireUserExists(UUID userId) {
        if (!userAccountRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User with id=%s does not exist!".formatted(userId));
        }
    }

    private UpdateComponentResponse updateResponse(Component component) {
        return new UpdateComponentResponse(component.getId(), "Component updated successfully!");
    }
}
