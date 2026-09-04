package com.ramy.bugreport.service;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ramy.bugreport.domain.Component;
import com.ramy.bugreport.dto.component.CreateComponentRequest;
import com.ramy.bugreport.dto.component.CreateComponentResponse;
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
}
