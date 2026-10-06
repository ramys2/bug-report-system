package com.ramy.bugreport.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
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
import com.ramy.bugreport.messaging.event.BugReportClosedEvent;
import com.ramy.bugreport.messaging.event.StatusChangedEvent;
import com.ramy.bugreport.repository.IBugReportRepository;
import com.ramy.bugreport.repository.IComponentRepository;
import com.ramy.bugreport.repository.ISoftwareProjectRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;
import jakarta.transaction.Transactional;

/**
 * Business logic for bug reports: reading, creating, updating field by field and closing them.
 *
 * <p>Methods that change a report run in a transaction. Closed reports are read-only: every update
 * rejects them. Notification events ({@link com.ramy.bugreport.messaging.event.IBugReportEvent}) are
 * published through Spring's event mechanism and only sent to the message queue after the
 * transaction commits.
 *
 * <p>{@code updatedAt} is set on creation and refreshed on every change (the column is NOT NULL).
 */
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

    /**
     * Returns all bug reports in brief form, with reporter and assignee names resolved.
     *
     * @throws ResourceNotFoundException if a report references a user that does not exist
     */
    public List<BugReportBriefResponse> getAll() {
        return mapToBriefResponses(bugReportRepository.findAll());
    }
    
    /**
     * Returns, for each status, the statuses that can be chosen as the next one with {@link #updateStatus}.
     * Statuses without such a target are left out. {@code ASSIGNED} and {@code CLOSED} are never listed, because
     * they are reached through {@link #updateAssignee} and {@link #close}.
     */
    public Map<EBugStatus, List<EBugStatus>> getStatusTransitions() {
        var transitions = new EnumMap<EBugStatus, List<EBugStatus>>(EBugStatus.class);
        for (var from : EBugStatus.values()) {
            var targets = Arrays.stream(EBugStatus.values())
                    .filter(from::canTransitionTo)
                    .filter(target -> target != EBugStatus.ASSIGNED && target != EBugStatus.CLOSED)
                    .toList();
            if (!targets.isEmpty()) {
                transitions.put(from, targets);
            }
        }
        return transitions;
    }

    /**
     * Returns the full detail of one report, including its reporter, assignee, project and component.
     *
     * @param reportId id of the report
     * @throws ResourceNotFoundException if the report, or any user, project or component it refers to, does not exist
     */
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
    
    /**
     * Returns the brief form of all reports filed by the given user (empty list if there are none).
     *
     * @param reporterId id of the reporting user
     */
    public List<BugReportBriefResponse> getReportsByReporter(UUID reporterId) {
        return mapToBriefResponses(bugReportRepository.findByReporterId(reporterId));
    }
    
    /**
     * Returns the brief form of all reports assigned to the given user, including closed ones.
     *
     * @param assigneeId id of the assigned user
     */
    public List<BugReportBriefResponse> getReportsByAssignee(UUID assigneeId) {
        return mapToBriefResponses(bugReportRepository.findByAssigneeId(assigneeId));
    }

    /** Converts reports to brief responses, loading all needed users with a single query instead of one per report. */
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
    
    /**
     * Creates a new report with the current time as {@code createdAt} and {@code updatedAt}.
     *
     * <p>The status is {@link EBugStatus#OPEN}, or {@link EBugStatus#ASSIGNED} if the request contains an assignee.
     * In that case an {@link com.ramy.bugreport.messaging.event.AssigneeChangedEvent} is published to the assignee.
     *
     * @param reporterId id of the user filing the report
     * @param request the report data; project, component, title and severity are required
     * @return the id of the new report
     * @throws ResourceNotFoundException if the reporter, project or component does not exist
     * @throws BusinessRuleConflictException if an assignee is given who is not a developer
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

        UserAccount assignee = null;
        if (request.assigneeId() != null) {
            assignee = requireDeveloper(request.assigneeId());
        }

        var now = LocalDateTime.now();
        BugReport report = BugReport.builder(reporterId, projectId, componentId, title, severity)
            .assigneeId(request.assigneeId())
            .description(request.description())
            .stepsToReproduce(request.stepsToReproduce())
            .expectedBehavior(request.expectedBehavior())
            .actualBehavior(request.actualBehavior())
            .createdAt(now)
            .updatedAt(now)
            .build();

        if (assignee != null) {
            report.setStatus(EBugStatus.ASSIGNED);
        }

        report = bugReportRepository.save(report);

        if (assignee != null) {
            eventPublisher.publishEvent(new AssigneeChangedEvent(
                    assignee.getName(), assignee.getEmailAddress(), report.getTitle()));
        }

        return new CreateBugReportResponse(report.getId(), "Successfully created!");
    }
    
    /**
     * Closes a report by attaching a resolution and setting its status to {@link EBugStatus#CLOSED}.
     *
     * <p>{@code resolvedAt} is set to the current time. A {@link com.ramy.bugreport.messaging.event.BugReportClosedEvent}
     * addressed to the reporter and, if present, the assignee is published.
     *
     * <p>Requires the ADMIN role, or the caller being the report's reporter or assignee
     * (checked by {@code BugReportAuthorizer.canUpdate}); otherwise access is denied.
     *
     * @param reportId id of the report to close
     * @param request resolution description, fixed version and commit URL
     * @return confirmation containing the report id
     * @throws ResourceNotFoundException if the report, its reporter or its assignee does not exist
     * @throws BusinessRuleConflictException if the report is already closed
     */
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

        var reporterId = report.getReporterId();
        var assigneeId = report.getAssigneeId();

        report = saveUpdated(report);

        var reporter = userAccountRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id=%s does not exist!".formatted(reporterId)));
        var assigneeEmail = assigneeId == null
                ? null
                : userAccountRepository.findById(assigneeId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "User with id=%s does not exist!".formatted(assigneeId)))
                        .getEmailAddress();
        eventPublisher.publishEvent(new BugReportClosedEvent(report.getTitle(), assigneeEmail, reporter.getEmailAddress()));

        return new CloseBugReportResponse(report.getId(), "Task has been closed successfully!");
    }
    
    /*
    * ============================================
    *
    * PATCH
    *
    * ============================================
    */
    
    /**
     * Assigns the report to a developer and publishes an {@link com.ramy.bugreport.messaging.event.AssigneeChangedEvent}.
     * An {@link EBugStatus#OPEN} report becomes {@link EBugStatus#ASSIGNED}; the status of any other report is not changed.
     *
     * <p>Requires the ADMIN role, or the caller being the report's reporter or assignee
     * (checked by {@code BugReportAuthorizer.canUpdate}); otherwise access is denied.
     *
     * @param reportId id of the report to change
     * @param request the new value
     * @return confirmation containing the report id
     * @throws ResourceNotFoundException if the report or the new assignee does not exist
     * @throws BusinessRuleConflictException if the report is closed or the new assignee is not a developer
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
        if (report.getStatus() == EBugStatus.OPEN) {
            report.setStatus(EBugStatus.ASSIGNED);
        }
        saveUpdated(report);
        eventPublisher.publishEvent(new AssigneeChangedEvent(
                assignee.getName(), assignee.getEmailAddress(), report.getTitle()));
        return updateResponse(report);
    }

    /**
     * Changes the severity.
     *
     * <p>Requires the ADMIN role, or the caller being the report's reporter or assignee
     * (checked by {@code BugReportAuthorizer.canUpdate}); otherwise access is denied.
     *
     * @param reportId id of the report to change
     * @param request the new value
     * @return confirmation containing the report id
     * @throws ResourceNotFoundException if the report does not exist
     * @throws BusinessRuleConflictException if the report is already closed
     */
    @Transactional
    @PreAuthorize(
            "hasRole('ADMIN') or @bugReportAuthorizer.canUpdate(#reportId, authentication)"
    )
    public UpdateBugReportResponse updateSeverity(UUID reportId, UpdateSeverityRequest request) {
        var report = reportById(reportId);
        report.setSeverity(request.severity());
        saveUpdated(report);
        return updateResponse(report);
    }

    /**
     * Changes the status and publishes a {@link com.ramy.bugreport.messaging.event.StatusChangedEvent}
     * to the reporter and, if present, the assignee. Setting the current status again changes nothing and publishes no event.
     *
     * <p>{@link EBugStatus#CLOSED} cannot be set here (use {@link #close}), and neither can {@link EBugStatus#ASSIGNED}
     * (use {@link #updateAssignee}). Other changes must be allowed by {@link EBugStatus#canTransitionTo}.
     *
     * <p>Requires the ADMIN role, or the caller being the report's reporter or assignee
     * (checked by {@code BugReportAuthorizer.canUpdate}); otherwise access is denied.
     *
     * @param reportId id of the report to change
     * @param request the new value
     * @return confirmation containing the report id
     * @throws ResourceNotFoundException if the report does not exist
     * @throws BusinessRuleConflictException if the report is already closed, the requested status is {@code CLOSED}
     *         or {@code ASSIGNED}, or the transition is not allowed
     */
    @Transactional
    @PreAuthorize(
            "hasRole('ADMIN') or @bugReportAuthorizer.canUpdate(#reportId, authentication)"
    )
    public UpdateBugReportResponse updateStatus(UUID reportId, UpdateStatusRequest request) {
        var report = reportById(reportId);

        if (request.status() == EBugStatus.CLOSED) {
            throw new BusinessRuleConflictException("Use the resolution endpoint to close a report.");
        }

        if (request.status() == EBugStatus.ASSIGNED) {
            throw new BusinessRuleConflictException("Use the assignee endpoint to assign a report.");
        }

        if (request.status() == report.getStatus()) {
            return updateResponse(report);
        }

        if (!report.getStatus().canTransitionTo(request.status())) {
            throw new BusinessRuleConflictException(
                    "Cannot change status from %s to %s.".formatted(report.getStatus(), request.status()));
        }

        report.setStatus(request.status());
        saveUpdated(report);

        var reporter = userAccountRepository.findById(report.getReporterId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id=%s does not exist!".formatted(report.getReporterId())));
        var assigneeEmail = report.getAssigneeId() == null
                ? null
                : userAccountRepository.findById(report.getAssigneeId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "User with id=%s does not exist!".formatted(report.getAssigneeId())))
                        .getEmailAddress();
        eventPublisher.publishEvent(new StatusChangedEvent(report.getTitle(), assigneeEmail, reporter.getEmailAddress()));
        return updateResponse(report);
    }

    /**
     * Moves the report to another project.
     *
     * <p>Requires the ADMIN role, or the caller being the report's reporter or assignee
     * (checked by {@code BugReportAuthorizer.canUpdate}); otherwise access is denied.
     *
     * @param reportId id of the report to change
     * @param request the new value
     * @return confirmation containing the report id
     * @throws ResourceNotFoundException if the report or the project does not exist
     * @throws BusinessRuleConflictException if the report is already closed
     */
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
        saveUpdated(report);
        return updateResponse(report);
    }

    /**
     * Moves the report to another component.
     * The component is not checked against the report's project.
     *
     * <p>Requires the ADMIN role, or the caller being the report's reporter or assignee
     * (checked by {@code BugReportAuthorizer.canUpdate}); otherwise access is denied.
     *
     * @param reportId id of the report to change
     * @param request the new value
     * @return confirmation containing the report id
     * @throws ResourceNotFoundException if the report or the component does not exist
     * @throws BusinessRuleConflictException if the report is already closed
     */
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
        saveUpdated(report);
        return updateResponse(report);
    }

    /**
     * Replaces the description.
     *
     * <p>Requires the ADMIN role, or the caller being the report's reporter or assignee
     * (checked by {@code BugReportAuthorizer.canUpdate}); otherwise access is denied.
     *
     * @param reportId id of the report to change
     * @param request the new value
     * @return confirmation containing the report id
     * @throws ResourceNotFoundException if the report does not exist
     * @throws BusinessRuleConflictException if the report is already closed
     */
    @Transactional
    @PreAuthorize(
            "hasRole('ADMIN') or @bugReportAuthorizer.canUpdate(#reportId, authentication)"
    )
    public UpdateBugReportResponse updateDescription(UUID reportId, UpdateDescriptionRequest request) {
        var report = reportById(reportId);
        report.setDescription(request.description());
        saveUpdated(report);
        return updateResponse(report);
    }

    /**
     * Replaces the steps to reproduce.
     *
     * <p>Requires the ADMIN role, or the caller being the report's reporter or assignee
     * (checked by {@code BugReportAuthorizer.canUpdate}); otherwise access is denied.
     *
     * @param reportId id of the report to change
     * @param request the new value
     * @return confirmation containing the report id
     * @throws ResourceNotFoundException if the report does not exist
     * @throws BusinessRuleConflictException if the report is already closed
     */
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
        saveUpdated(report);
        return updateResponse(report);
    }

    /**
     * Replaces the expected behavior.
     *
     * <p>Requires the ADMIN role, or the caller being the report's reporter or assignee
     * (checked by {@code BugReportAuthorizer.canUpdate}); otherwise access is denied.
     *
     * @param reportId id of the report to change
     * @param request the new value
     * @return confirmation containing the report id
     * @throws ResourceNotFoundException if the report does not exist
     * @throws BusinessRuleConflictException if the report is already closed
     */
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
        saveUpdated(report);
        return updateResponse(report);
    }

    /**
     * Replaces the actual behavior.
     *
     * <p>Requires the ADMIN role, or the caller being the report's reporter or assignee
     * (checked by {@code BugReportAuthorizer.canUpdate}); otherwise access is denied.
     *
     * @param reportId id of the report to change
     * @param request the new value
     * @return confirmation containing the report id
     * @throws ResourceNotFoundException if the report does not exist
     * @throws BusinessRuleConflictException if the report is already closed
     */
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
        saveUpdated(report);
        return updateResponse(report);
    }

    /** Loads a report for modification; unlike a plain lookup it also rejects closed reports. */
    private BugReport reportById(UUID reportId) {
        var report = bugReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report with id: %s".formatted(reportId)));

        if (report.getStatus() == EBugStatus.CLOSED) {
            throw new BusinessRuleConflictException("Report is closed and cannot be updated.");
        }

        return report;
    }

    /** Sets {@code updatedAt} to the current time and saves the report. */
    private BugReport saveUpdated(BugReport report) {
        report.setUpdatedAt(LocalDateTime.now());
        return bugReportRepository.save(report);
    }

    private UpdateBugReportResponse updateResponse(BugReport report) {
        return new UpdateBugReportResponse(report.getId(), "Bug report updated successfully!");
    }

    /** Loads the user and checks that they have the {@code DEVELOPER} role (admins are not accepted). */
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
