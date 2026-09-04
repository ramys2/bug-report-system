package com.ramy.bugreport.service;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ramy.bugreport.domain.SoftwareProject;
import com.ramy.bugreport.dto.project.CreateSoftwareProjectRequest;
import com.ramy.bugreport.dto.project.CreateSoftwareProjectResponse;
import com.ramy.bugreport.dto.project.UpdateSoftwareProjectDescriptionRequest;
import com.ramy.bugreport.dto.project.UpdateSoftwareProjectNameRequest;
import com.ramy.bugreport.dto.project.UpdateSoftwareProjectResponse;
import com.ramy.bugreport.exception.ResourceNotFoundException;
import com.ramy.bugreport.repository.ISoftwareProjectRepository;

import jakarta.transaction.Transactional;

@Service
public class SoftwareProjectService {
    private final ISoftwareProjectRepository softwareProjectRepository;

    public SoftwareProjectService(ISoftwareProjectRepository softwareProjectRepository) {
        this.softwareProjectRepository = softwareProjectRepository;
    }

    /*
    * ============================================
    *
    * GET
    *
    * ============================================
    */

    public Map<UUID, String> getAll() {
        return softwareProjectRepository.findAll()
                .stream()
                .collect(Collectors.toMap(SoftwareProject::getId, SoftwareProject::getName));
    }

    /*
    * ============================================
    *
    * POST
    *
    * ============================================
    */

    @Transactional
    public CreateSoftwareProjectResponse create(CreateSoftwareProjectRequest request) {
        SoftwareProject project = new SoftwareProject(request.name(), request.description());
        project = softwareProjectRepository.save(project);

        return new CreateSoftwareProjectResponse(project.getId(), "Successfully created!");
    }

    /*
    * ============================================
    *
    * PATCH
    *
    * ============================================
    */

    @Transactional
    public UpdateSoftwareProjectResponse updateName(
            UUID projectId,
            UpdateSoftwareProjectNameRequest request
    ) {
        var project = projectById(projectId);
        project.setName(request.name());
        return updateResponse(project);
    }

    @Transactional
    public UpdateSoftwareProjectResponse updateDescription(
            UUID projectId,
            UpdateSoftwareProjectDescriptionRequest request
    ) {
        var project = projectById(projectId);
        project.setDescription(request.description());
        return updateResponse(project);
    }

    private SoftwareProject projectById(UUID projectId) {
        return softwareProjectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project with id=%s does not exist!".formatted(projectId)));
    }

    private UpdateSoftwareProjectResponse updateResponse(SoftwareProject project) {
        return new UpdateSoftwareProjectResponse(project.getId(), "Project updated successfully!");
    }
}
