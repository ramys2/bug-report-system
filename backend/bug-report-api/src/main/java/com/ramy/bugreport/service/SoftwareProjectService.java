package com.ramy.bugreport.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ramy.bugreport.domain.SoftwareProject;
import com.ramy.bugreport.dto.project.CreateSoftwareProjectRequest;
import com.ramy.bugreport.dto.project.CreateSoftwareProjectResponse;
import com.ramy.bugreport.dto.project.SoftwareProjectResponse;
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

    public List<SoftwareProjectResponse> getAll() {
        return softwareProjectRepository.findAll()
                .stream()
                .map(SoftwareProjectResponse::from)
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
        softwareProjectRepository.save(project);
        return updateResponse(project);
    }

    @Transactional
    public UpdateSoftwareProjectResponse updateDescription(
            UUID projectId,
            UpdateSoftwareProjectDescriptionRequest request
    ) {
        var project = projectById(projectId);
        project.setDescription(request.description());
        softwareProjectRepository.save(project);
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
