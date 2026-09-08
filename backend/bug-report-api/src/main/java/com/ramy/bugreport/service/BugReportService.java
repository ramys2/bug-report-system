package com.ramy.bugreport.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.domain.EBugStatus;
import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.Resolution;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.dto.report.BugReportBriefResponse;
import com.ramy.bugreport.dto.report.BugReportResponse;
import com.ramy.bugreport.dto.report.CloseBugReportRequest;
import com.ramy.bugreport.dto.report.CloseBugReportResponse;
import com.ramy.bugreport.dto.report.CreateBugReportRequest;
import com.ramy.bugreport.dto.report.CreateBugReportResponse;
import com.ramy.bugreport.dto.report.UpdateActualBehaviorRequest;
import com.ramy.bugreport.dto.report.UpdateAssigneeRequest;
import com.ramy.bugreport.dto.report.UpdateBugReportResponse;
import com.ramy.bugreport.dto.report.UpdateComponentRequest;
import com.ramy.bugreport.dto.report.UpdateDescriptionRequest;
import com.ramy.bugreport.dto.report.UpdateExpectedBehaviorRequest;
import com.ramy.bugreport.dto.report.UpdateProjectRequest;
import com.ramy.bugreport.dto.report.UpdateSeverityRequest;
import com.ramy.bugreport.dto.report.UpdateStatusRequest;
import com.ramy.bugreport.dto.report.UpdateStepsToReproduceRequest;
import com.ramy.bugreport.exception.BusinessRuleConflictException;
import com.ramy.bugreport.exception.ResourceNotFoundException;
import com.ramy.bugreport.messaging.event.AssigneeChangedEvent;
import com.ramy.bugreport.messaging.publisher.BugReportEventPublisher;
import com.ramy.bugreport.repository.IBugReportRepository;
import com.ramy.bugreport.repository.IComponentRepository;
import com.ramy.bugreport.repository.ISoftwareProjectRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;
import jakarta.transaction.Transactional;

@Service
public class BugReportService {
    private final IBugReportRepository bugReportRepository;
    private final IUserAccountRepository userAccountRepository;
    private final ISoftwareProjectRepository softwareProjectRepository;
    private final IComponentRepository componentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BugReportService(
        IBugReportRepository bugBugReportRepository,
        IUserAccountRepository userAccountRepository,
        ISoftwareProjectRepository softwareProjectRepository,
        IComponentRepository componentRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.bugReportRepository = bugBugReportRepository;
        this.userAccountRepository = userAccountRepository;
        this.softwareProjectRepository = softwareProjectRepository;
        this.componentRepository = componentRepository;
        this.eventPublisher = eventPublisher;
    }
    
    /*
    * ============================================
    *
    * GET
    *
    * ============================================
    */

    public List<BugReportBriefResponse> getAll() {
        return mapToBriefResponses(bugReportRepository.findAll());
    }
    
    public BugReportResponse getReport(UUID reportId) {
        BugReport report = bugReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report with id: %s".formatted(reportId)));

        var reporter = userAccountRepository.findById(report.getReporterId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id=%s does not exist!".formatted(report.getReporterId())));
        var assignee = report.getAssigneeId() == null
                ? null
                : userAccountRepository.findById(report.getAssigneeId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "User with id=%s does not exist!".formatted(report.getAssigneeId())));
        var project = softwareProjectRepository.findById(report.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project with id=%s does not exist!".formatted(report.getProjectId())));
        var component = componentRepository.findById(report.getComponentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Component with id=%s does not exist!".formatted(report.getComponentId())));

        return BugReportResponse.from(report, reporter, assignee, project, component);
    }
    
    public List<BugReportBriefResponse> getReportsByReporter(UUID reporterId) {
        return mapToBriefResponses(bugReportRepository.findByReporterId(reporterId));
    }
    
    public List<BugReportBriefResponse> getReportsByAssignee(UUID assigneeId) {
        return mapToBriefResponses(bugReportRepository.findByAssigneeId(assigneeId));
    }

    private List<BugReportBriefResponse> mapToBriefResponses(List<BugReport> reports) {
        var userIds = reports.stream()
                .flatMap(report -> java.util.stream.Stream.of(report.getReporterId(), report.getAssigneeId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, UserAccount> usersById = userAccountRepository.findAllById(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(UserAccount::getId, Function.identity()));

        return reports.stream()
                .map(report -> BugReportBriefResponse.from(
                        report,
                        requiredUser(usersById, report.getReporterId()),
                        report.getAssigneeId() == null
                                ? null
                                : requiredUser(usersById, report.getAssigneeId())))
                .toList();
    }

    private UserAccount requiredUser(Map<UUID, UserAccount> usersById, UUID userId) {
        var user = usersById.get(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User with id=%s does not exist!".formatted(userId));
        }
        return user;
    }

    
    /*
    * ============================================
    *
    * POST
    *
    * ============================================
    */
    
    @Transactional
    public CreateBugReportResponse create(UUID reporterId, CreateBugReportRequest request) {
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

        if (request.assigneeId() != null) {
            requireDeveloper(request.assigneeId());
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
    @PreAuthorize(
    	"hasRole('ADMIN') or @bugReportAuthorizer.canUpdate(#reportId, authentication)"
    )
    public CloseBugReportResponse close(UUID reportId, CloseBugReportRequest request) {
        BugReport report = bugReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report with id: %s".formatted(reportId)));

        if (report.getStatus() == EBugStatus.CLOSED || report.getResolution() != null) {
            throw new BusinessRuleConflictException("Report is already closed and cannot be reopened.");
        }
        
        var resolution = new Resolution(request.description(), LocalDateTime.now(), request.fixedVersion(), request.commitUrl());
        
        report.setResolution(resolution);
        report.setStatus(EBugStatus.CLOSED);
        
        report = bugReportRepository.save(report);
        
        return new CloseBugReportResponse(report.getResolution().getId(), "Task has been closed successfully!");
    }
    
    /*
    * ============================================
    *
    * PATCH
    *
    * ============================================
    */
    
    @Transactional
    @PreAuthorize(
    		"hasRole('ADMIN') or @bugReportAuthorizer.canUpdate(#reportId, authentication)"
    )
    public UpdateBugReportResponse updateAssignee(UUID reportId, UpdateAssigneeRequest request) {
        var report = reportById(reportId);
        var assigneeId = request.assigneeId();
        var assignee = requireDeveloper(assigneeId);

        report.setAssigneeId(assigneeId);
        bugReportRepository.save(report);
        eventPublisher.publishEvent(new AssigneeChangedEvent(assignee.getName(), assignee.getEmailAddress(), report.getTitle()));
        return updateResponse(report);
    }

    @Transactional
    @PreAuthorize(
            "hasRole('ADMIN') or @bugReportAuthorizer.canUpdate(#reportId, authentication)"
    )
    public UpdateBugReportResponse updateSeverity(UUID reportId, UpdateSeverityRequest request) {
        var report = reportById(reportId);
        report.setSeverity(request.severity());
        bugReportRepository.save(report);
        return updateResponse(report);
    }

    @Transactional
    @PreAuthorize(
            "hasRole('ADMIN') or @bugReportAuthorizer.canUpdate(#reportId, authentication)"
    )
    public UpdateBugReportResponse updateStatus(UUID reportId, UpdateStatusRequest request) {
        var report = reportById(reportId);

        if (request.status() == EBugStatus.CLOSED) {
            throw new BusinessRuleConflictException("Use the resolution endpoint to close a report.");
        }

        report.setStatus(request.status());
        bugReportRepository.save(report);
        return updateResponse(report);
    }

    @Transactional
    @PreAuthorize(
            "hasRole('ADMIN') or @bugReportAuthorizer.canUpdate(#reportId, authentication)"
    )
    public UpdateBugReportResponse updateProject(UUID reportId, UpdateProjectRequest request) {
        var report = reportById(reportId);
        var projectId = request.projectId();
        if (!softwareProjectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project with id=%s does not exist!".formatted(projectId));
        }

        report.setProjectId(projectId);
        bugReportRepository.save(report);
        return updateResponse(report);
    }

    @Transactional
    @PreAuthorize(
            "hasRole('ADMIN') or @bugReportAuthorizer.canUpdate(#reportId, authentication)"
    )
    public UpdateBugReportResponse updateComponent(UUID reportId, UpdateComponentRequest request) {
        var report = reportById(reportId);
        var componentId = request.componentId();
        if (!componentRepository.existsById(componentId)) {
            throw new ResourceNotFoundException("Component with id=%s does not exist!".formatted(componentId));
        }

        report.setComponentId(componentId);
        bugReportRepository.save(report);
        return updateResponse(report);
    }

    @Transactional
    @PreAuthorize(
            "hasRole('ADMIN') or @bugReportAuthorizer.canUpdate(#reportId, authentication)"
    )
    public UpdateBugReportResponse updateDescription(UUID reportId, UpdateDescriptionRequest request) {
        var report = reportById(reportId);
        report.setDescription(request.description());
        bugReportRepository.save(report);
        return updateResponse(report);
    }

    @Transactional
    @PreAuthorize(
            "hasRole('ADMIN') or @bugReportAuthorizer.canUpdate(#reportId, authentication)"
    )
    public UpdateBugReportResponse updateStepsToReproduce(
            UUID reportId,
            UpdateStepsToReproduceRequest request
    ) {
        var report = reportById(reportId);
        report.setStepsToReproduce(request.stepsToReproduce());
        bugReportRepository.save(report);
        return updateResponse(report);
    }

    @Transactional
    @PreAuthorize(
            "hasRole('ADMIN') or @bugReportAuthorizer.canUpdate(#reportId, authentication)"
    )
    public UpdateBugReportResponse updateExpectedBehavior(
            UUID reportId,
            UpdateExpectedBehaviorRequest request
    ) {
        var report = reportById(reportId);
        report.setExpectedBehavior(request.expectedBehavior());
        bugReportRepository.save(report);
        return updateResponse(report);
    }

    @Transactional
    @PreAuthorize(
            "hasRole('ADMIN') or @bugReportAuthorizer.canUpdate(#reportId, authentication)"
    )
    public UpdateBugReportResponse updateActualBehavior(
            UUID reportId,
            UpdateActualBehaviorRequest request
    ) {
        var report = reportById(reportId);
        report.setActualBehavior(request.actualBehavior());
        bugReportRepository.save(report);
        return updateResponse(report);
    }

    private BugReport reportById(UUID reportId) {
        var report = bugReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report with id: %s".formatted(reportId)));

        if (report.getStatus() == EBugStatus.CLOSED) {
            throw new BusinessRuleConflictException("Report is closed and cannot be updated.");
        }

        return report;
    }

    private UpdateBugReportResponse updateResponse(BugReport report) {
        return new UpdateBugReportResponse(report.getId(), "Bug report updated successfully!");
    }

    private UserAccount requireDeveloper(UUID assigneeId) {
        UserAccount assignee = userAccountRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id=%s does not exist!".formatted(assigneeId)));
        if (assignee.getRole() != EUserRole.DEVELOPER) {
            throw new BusinessRuleConflictException(
                    "Bug reports can only be assigned to developers.");
        }
        
        return assignee;
    }
}
