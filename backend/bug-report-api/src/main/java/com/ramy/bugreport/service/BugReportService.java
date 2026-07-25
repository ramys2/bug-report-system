package com.ramy.bugreport.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.dto.report.BugReportResponse;
import com.ramy.bugreport.dto.report.CreateBugReportRequest;
import com.ramy.bugreport.dto.report.CreateBugReportResponse;
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
}
