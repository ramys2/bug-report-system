package com.ramy.bugreport.service;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ramy.bugreport.domain.SoftwareProject;
import com.ramy.bugreport.dto.project.CreateSoftwareProjectRequest;
import com.ramy.bugreport.dto.project.CreateSoftwareProjectResponse;
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
}
