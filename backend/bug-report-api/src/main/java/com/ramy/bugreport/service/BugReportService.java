package com.ramy.bugreport.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.domain.Resolution;
import com.ramy.bugreport.dto.report.BugReportResponse;
import com.ramy.bugreport.dto.report.CloseBugReportRequest;
import com.ramy.bugreport.dto.report.CloseBugReportResponse;
import com.ramy.bugreport.dto.report.CreateBugReportRequest;
import com.ramy.bugreport.dto.report.CreateBugReportResponse;
import com.ramy.bugreport.dto.report.UpdateBugReportRequest;
import com.ramy.bugreport.dto.report.UpdateBugReportResponse;
import com.ramy.bugreport.exception.ResourceNotFoundException;
import com.ramy.bugreport.repository.IBugReportRepository;
import com.ramy.bugreport.repository.IComponentRepository;
import com.ramy.bugreport.repository.ISoftwareProjectRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;

import jakarta.transaction.Transactional;
import java.util.UUID;

@Service
public class BugReportService {
    private final IBugReportRepository bugReportRepository;
    private final IUserAccountRepository userAccountRepository;
    private final ISoftwareProjectRepository softwareProjectRepository;
    private final IComponentRepository componentRepository;

    public BugReportService(
        IBugReportRepository bugBugReportRepository,
        IUserAccountRepository userAccountRepository,
        ISoftwareProjectRepository softwareProjectRepository,
        IComponentRepository componentRepository
    ) {
        this.bugReportRepository = bugBugReportRepository;
        this.userAccountRepository = userAccountRepository;
        this.softwareProjectRepository = softwareProjectRepository;
        this.componentRepository = componentRepository;
    }
    
    /*
    * ============================================
    *
    * GET
    *
    * ============================================
    */

    public List<BugReportResponse> getAll() {
        return bugReportRepository.findAll()
            .stream()
            .map(BugReportResponse::from)
            .toList();
    }
    
    public BugReportResponse getReport(UUID reportId) {
        BugReport report = bugReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report with id: %s".formatted(reportId)));
        
        return BugReportResponse.from(report);
    }
    
    public List<BugReportResponse> getReportsByReporter(UUID reporterId) {
        return bugReportRepository.findByReporterId(reporterId)
                .stream()
                .map(BugReportResponse::from)
                .toList();
    }
    
    public List<BugReportResponse> getReportsByAssignee(UUID assigneeId) {
        return bugReportRepository.findByAssigneeId(assigneeId)
                .stream()
                .map(BugReportResponse::from)
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
    public CreateBugReportResponse create(CreateBugReportRequest request) {
        var reporterId = request.reporterId();
        var projectId = request.projectId();
        var componentId = request.componentId();
        var title = request.title();
        var severity = request.severity();

        if (!userAccountRepository.existsById(reporterId)) {
            throw new ResourceNotFoundException(
                    "User with id=%s does not exist!".formatted(reporterId)
            );
        }
    
        if (!softwareProjectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException(
                    "Project with id=%s does not exist!".formatted(projectId)
            );
        }
    
        if (!componentRepository.existsById(componentId)) {
            throw new ResourceNotFoundException(
                    "Component with id=%s does not exist!".formatted(componentId)
            );
        }

        BugReport report = BugReport.builder(reporterId, projectId, componentId, title, severity)
            .assigneeId(request.assigneeId())
            .description(request.description())
            .stepsToReproduce(request.stepsToReproduce())
            .expectedBehavior(request.expectedBehavior())
            .actualBehavior(request.actualBehavior())
            .createdAt(LocalDateTime.now())
            .build();

        report = bugReportRepository.save(report);

        return new CreateBugReportResponse(report.getId(), "Successfully created!");
    }
    
    @Transactional
    public CloseBugReportResponse close(UUID reportId, CloseBugReportRequest request) {
        BugReport report = bugReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report with id: %s".formatted(reportId)));
        
        var resolution = new Resolution(request.description(), LocalDateTime.now(), request.fixedVersion(), request.commitUrl());
        
        report.setResolution(resolution);
        
        bugReportRepository.save(report);
        
        return new CloseBugReportResponse(resolution.getId(), "Task has been closed successfully!");
    }
    
    /*
    * ============================================
    *
    * PATCH
    *
    * ============================================
    */
    
    @Transactional
    public UpdateBugReportResponse update(UUID reportId, UpdateBugReportRequest request) {
        BugReport report = bugReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report with id: %s".formatted(reportId)));
        
        var assigneedId = request.assigneeId();
        if (assigneedId != null) {
            if (!userAccountRepository.existsById(assigneedId)) {
                throw new ResourceNotFoundException(
                        "User with id=%s does not exist!".formatted(assigneedId)
                );
            }

            report.setAssigneeId(request.assigneeId());
        }
        
        if (request.description() != null) {
            report.setDescription(request.description());
        }
        
        if (request.stepsToReproduce() != null) {
            report.setStepsToReproduce(request.stepsToReproduce());
        }
        
        if (request.expectedBehavior() != null) {
            report.setExpectedBehavior(request.expectedBehavior());
        }
        
        if (request.actualBehavior() != null) {
            report.setActualBehavior(request.actualBehavior());
        }
        
        if (request.bugStatus() != null) {
            report.setStatus(request.bugStatus());
        }
        
        return new UpdateBugReportResponse(report.getId(), "Bug report updated successfully!");
    }
}
