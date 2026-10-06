package com.ramy.bugreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.domain.Component;
import com.ramy.bugreport.domain.EBugSeverity;
import com.ramy.bugreport.domain.EBugStatus;
import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.SoftwareProject;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.dto.report.CloseBugReportRequest;
import com.ramy.bugreport.dto.report.CreateBugReportRequest;
import com.ramy.bugreport.dto.report.UpdateActualBehaviorRequest;
import com.ramy.bugreport.dto.report.UpdateAssigneeRequest;
import com.ramy.bugreport.dto.report.UpdateComponentRequest;
import com.ramy.bugreport.dto.report.UpdateDescriptionRequest;
import com.ramy.bugreport.dto.report.UpdateExpectedBehaviorRequest;
import com.ramy.bugreport.dto.report.UpdateProjectRequest;
import com.ramy.bugreport.dto.report.UpdateSeverityRequest;
import com.ramy.bugreport.dto.report.UpdateStatusRequest;
import com.ramy.bugreport.dto.report.UpdateStepsToReproduceRequest;
import com.ramy.bugreport.messaging.event.AssigneeChangedEvent;
import com.ramy.bugreport.exception.ResourceNotFoundException;
import com.ramy.bugreport.exception.BusinessRuleConflictException;
import com.ramy.bugreport.repository.IBugReportRepository;
import com.ramy.bugreport.repository.IComponentRepository;
import com.ramy.bugreport.repository.ISoftwareProjectRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;

@ExtendWith(MockitoExtension.class)
class BugReportServiceTest {

    @Mock
    private IBugReportRepository bugReportRepository;
    @Mock
    private IUserAccountRepository userAccountRepository;
    @Mock
    private ISoftwareProjectRepository softwareProjectRepository;
    @Mock
    private IComponentRepository componentRepository;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private BugReportService service;

    @BeforeEach
    void setUp() {
        service = new BugReportService(
                bugReportRepository,
                userAccountRepository,
                softwareProjectRepository,
                componentRepository,
                eventPublisher);
    }

    @Test
    void getAllMapsReportsToBriefResponses() {
        var first = report(UUID.randomUUID());
        var second = report(UUID.randomUUID());
        first.setCreatedAt(LocalDateTime.of(2026, 7, 13, 12, 5));
        stubUsers(first, second);
        when(bugReportRepository.findAll()).thenReturn(List.of(first, second));

        var result = service.getAll();

        assertThat(result).extracting(response -> response.reportId())
                .containsExactly(first.getId(), second.getId());
        assertThat(result.getFirst())
                .extracting("title", "author", "assignee", "status", "severity", "createdAt")
                .containsExactly(
                        "Application crashes", "Reporter", "Assignee", EBugStatus.OPEN, EBugSeverity.HIGH,
                        LocalDateTime.of(2026, 7, 13, 12, 5));
        verify(bugReportRepository).findAll();
    }

    @Test
    void getStatusTransitionsListsSelectableTargetsOnly() {
        var result = service.getStatusTransitions();

        assertThat(result).containsOnly(
                Map.entry(EBugStatus.ASSIGNED, List.of(EBugStatus.IN_PROGRESS, EBugStatus.NEEDS_INFORMATION)),
                Map.entry(EBugStatus.IN_PROGRESS, List.of(EBugStatus.NEEDS_INFORMATION, EBugStatus.REVIEWING)),
                Map.entry(EBugStatus.NEEDS_INFORMATION, List.of(EBugStatus.IN_PROGRESS)),
                Map.entry(EBugStatus.REVIEWING, List.of(EBugStatus.IN_PROGRESS)));
    }

    @Test
    void getReportReturnsMappedReport() {
        var report = report(UUID.randomUUID());
        var reporter = namedUser("Joe Reporter");
        var assignee = namedUser("Joe Developer");
        var project = project("Bug Report");
        var component = component("Backend API");
        var assigneeId = UUID.randomUUID();
        var projectId = UUID.randomUUID();
        var componentId = UUID.randomUUID();
        when(assignee.getId()).thenReturn(assigneeId);
        when(project.getId()).thenReturn(projectId);
        when(component.getId()).thenReturn(componentId);
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(userAccountRepository.findById(report.getReporterId())).thenReturn(Optional.of(reporter));
        when(userAccountRepository.findById(report.getAssigneeId())).thenReturn(Optional.of(assignee));
        when(softwareProjectRepository.findById(report.getProjectId())).thenReturn(Optional.of(project));
        when(componentRepository.findById(report.getComponentId())).thenReturn(Optional.of(component));

        var result = service.getReport(report.getId());

        assertThat(result.id()).isEqualTo(report.getId());
        assertThat(result.title()).isEqualTo(report.getTitle());
        assertThat(result.reporterName()).isEqualTo("Joe Reporter");
        assertThat(result.assigneeId()).isEqualTo(assigneeId);
        assertThat(result.assigneeName()).isEqualTo("Joe Developer");
        assertThat(result.projectId()).isEqualTo(projectId);
        assertThat(result.projectName()).isEqualTo("Bug Report");
        assertThat(result.componentId()).isEqualTo(componentId);
        assertThat(result.componentName()).isEqualTo("Backend API");
        verify(bugReportRepository).findById(report.getId());
        verify(userAccountRepository).findById(report.getReporterId());
        verify(userAccountRepository).findById(report.getAssigneeId());
        verify(softwareProjectRepository).findById(report.getProjectId());
        verify(componentRepository).findById(report.getComponentId());
    }

    @Test
    void getReportReturnsNullAssigneeNameWhenReportIsUnassigned() {
        var report = report(UUID.randomUUID());
        report.setAssigneeId(null);
        var reporter = namedUser("Joe Reporter");
        var project = project("Bug Report");
        var component = component("Backend API");
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(userAccountRepository.findById(report.getReporterId())).thenReturn(Optional.of(reporter));
        when(softwareProjectRepository.findById(report.getProjectId())).thenReturn(Optional.of(project));
        when(componentRepository.findById(report.getComponentId())).thenReturn(Optional.of(component));

        var result = service.getReport(report.getId());

        assertThat(result.assigneeId()).isNull();
        assertThat(result.assigneeName()).isNull();
        verify(userAccountRepository).findById(report.getReporterId());
        verifyNoMoreInteractions(userAccountRepository);
        verify(softwareProjectRepository).findById(report.getProjectId());
        verify(componentRepository).findById(report.getComponentId());
    }

    @Test
    void getReportReturnsNullComponentWhenReportHasNone() {
        var report = report(UUID.randomUUID());
        report.setComponentId(null);
        var reporter = namedUser("Joe Reporter");
        var assignee = namedUser("Joe Developer");
        var project = project("Bug Report");
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(userAccountRepository.findById(report.getReporterId())).thenReturn(Optional.of(reporter));
        when(userAccountRepository.findById(report.getAssigneeId())).thenReturn(Optional.of(assignee));
        when(softwareProjectRepository.findById(report.getProjectId())).thenReturn(Optional.of(project));

        var result = service.getReport(report.getId());

        assertThat(result.componentId()).isNull();
        assertThat(result.componentName()).isNull();
        verifyNoInteractions(componentRepository);
    }

    @Test
    void getReportThrowsWhenReportDoesNotExist() {
        var reportId = UUID.randomUUID();
        when(bugReportRepository.findById(reportId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReport(reportId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Report with id: %s".formatted(reportId));
    }

    @Test
    void getReportsByReporterMapsRepositoryResult() {
        var reporterId = UUID.randomUUID();
        var report = report(UUID.randomUUID());
        stubUsers(report);
        when(bugReportRepository.findByReporterId(reporterId)).thenReturn(List.of(report));

        assertThat(service.getReportsByReporter(reporterId))
                .extracting(response -> response.reportId())
                .containsExactly(report.getId());
        verify(bugReportRepository).findByReporterId(reporterId);
    }

    @Test
    void getReportsByAssigneeMapsRepositoryResult() {
        var assigneeId = UUID.randomUUID();
        var report = report(UUID.randomUUID());
        stubUsers(report);
        when(bugReportRepository.findByAssigneeId(assigneeId)).thenReturn(List.of(report));

        assertThat(service.getReportsByAssignee(assigneeId))
                .extracting(response -> response.reportId())
                .containsExactly(report.getId());
        verify(bugReportRepository).findByAssigneeId(assigneeId);
    }

    @Test
    void createValidatesReferencesBuildsAndSavesReport() {
        var request = createRequest();
        var reporterId = UUID.randomUUID();
        var savedId = UUID.randomUUID();
        when(userAccountRepository.existsById(reporterId)).thenReturn(true);
        when(softwareProjectRepository.existsById(request.projectId())).thenReturn(true);
        var component = componentOfProject(request.projectId());
        when(componentRepository.findById(request.componentId())).thenReturn(Optional.of(component));
        var developer = mock(UserAccount.class);
        when(developer.getRole()).thenReturn(EUserRole.DEVELOPER);
        when(userAccountRepository.findById(request.assigneeId())).thenReturn(Optional.of(developer));
        when(bugReportRepository.save(any(BugReport.class))).thenAnswer(invocation -> {
            BugReport report = invocation.getArgument(0);
            report.setId(savedId);
            return report;
        });
        var before = LocalDateTime.now();

        var result = service.create(reporterId, request);

        var reportCaptor = ArgumentCaptor.forClass(BugReport.class);
        verify(userAccountRepository).existsById(reporterId);
        verify(softwareProjectRepository).existsById(request.projectId());
        verify(componentRepository).findById(request.componentId());
        verify(bugReportRepository).save(reportCaptor.capture());
        var savedReport = reportCaptor.getValue();
        assertThat(savedReport.getReporterId()).isEqualTo(reporterId);
        assertThat(savedReport.getAssigneeId()).isEqualTo(request.assigneeId());
        assertThat(savedReport.getProjectId()).isEqualTo(request.projectId());
        assertThat(savedReport.getComponentId()).isEqualTo(request.componentId());
        assertThat(savedReport.getTitle()).isEqualTo(request.title());
        assertThat(savedReport.getDescription()).isEqualTo(request.description());
        assertThat(savedReport.getStepsToReproduce()).isEqualTo(request.stepsToReproduce());
        assertThat(savedReport.getExpectedBehavior()).isEqualTo(request.expectedBehavior());
        assertThat(savedReport.getActualBehavior()).isEqualTo(request.actualBehavior());
        assertThat(savedReport.getSeverity()).isEqualTo(request.severity());
        assertThat(savedReport.getStatus()).isEqualTo(EBugStatus.ASSIGNED);
        verify(eventPublisher).publishEvent(any(AssigneeChangedEvent.class));
        assertThat(savedReport.getCreatedAt()).isBetween(before, LocalDateTime.now());
        assertThat(result.id()).isEqualTo(savedId);
        assertThat(result.message()).isEqualTo("Successfully created!");
    }

    @Test
    void createThrowsWhenReporterDoesNotExist() {
        var request = createRequest();
        var reporterId = UUID.randomUUID();
        when(userAccountRepository.existsById(reporterId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(reporterId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User with id=%s does not exist!".formatted(reporterId));
        verify(userAccountRepository).existsById(reporterId);
        verifyNoInteractions(softwareProjectRepository, componentRepository, bugReportRepository);
    }

    @Test
    void createThrowsWhenProjectDoesNotExist() {
        var request = createRequest();
        var reporterId = UUID.randomUUID();
        when(userAccountRepository.existsById(reporterId)).thenReturn(true);
        when(softwareProjectRepository.existsById(request.projectId())).thenReturn(false);

        assertThatThrownBy(() -> service.create(reporterId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project with id=%s does not exist!".formatted(request.projectId()));
        verify(userAccountRepository).existsById(reporterId);
        verify(softwareProjectRepository).existsById(request.projectId());
        verifyNoInteractions(componentRepository, bugReportRepository);
    }

    @Test
    void createThrowsWhenComponentDoesNotExist() {
        var request = createRequest();
        var reporterId = UUID.randomUUID();
        when(userAccountRepository.existsById(reporterId)).thenReturn(true);
        when(softwareProjectRepository.existsById(request.projectId())).thenReturn(true);
        when(componentRepository.findById(request.componentId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(reporterId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Component with id=%s does not exist!".formatted(request.componentId()));
        verify(userAccountRepository).existsById(reporterId);
        verify(softwareProjectRepository).existsById(request.projectId());
        verify(componentRepository).findById(request.componentId());
        verify(bugReportRepository, never()).save(any());
    }

    @Test
    void createThrowsWhenComponentBelongsToAnotherProject() {
        var request = createRequest();
        var reporterId = UUID.randomUUID();
        when(userAccountRepository.existsById(reporterId)).thenReturn(true);
        when(softwareProjectRepository.existsById(request.projectId())).thenReturn(true);
        var component = componentOfProject(UUID.randomUUID());
        when(componentRepository.findById(request.componentId())).thenReturn(Optional.of(component));

        assertThatThrownBy(() -> service.create(reporterId, request))
                .isInstanceOf(BusinessRuleConflictException.class)
                .hasMessage("Component with id=%s does not belong to project with id=%s."
                        .formatted(request.componentId(), request.projectId()));
        verify(bugReportRepository, never()).save(any());
    }

    @Test
    void createAllowsReportWithoutComponent() {
        var request = new CreateBugReportRequest(
                null, UUID.randomUUID(), null, "Application crashes", null, null, null, null, EBugSeverity.HIGH);
        var reporterId = UUID.randomUUID();
        when(userAccountRepository.existsById(reporterId)).thenReturn(true);
        when(softwareProjectRepository.existsById(request.projectId())).thenReturn(true);
        when(bugReportRepository.save(any(BugReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(reporterId, request);

        var reportCaptor = ArgumentCaptor.forClass(BugReport.class);
        verify(bugReportRepository).save(reportCaptor.capture());
        assertThat(reportCaptor.getValue().getComponentId()).isNull();
        verifyNoInteractions(componentRepository);
    }

    @Test
    void closeAddsResolutionAndSavesReport() {
        var report = report(UUID.randomUUID());
        var request = new CloseBugReportRequest("Fixed null handling", "1.1.0", "https://example.com/commit/1");
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        var resolutionId = UUID.randomUUID();
        when(bugReportRepository.save(report)).thenAnswer(invocation -> {
            var saved = report(report.getId());
            var resolution = report.getResolution();
            saved.setResolution(new com.ramy.bugreport.domain.Resolution(resolutionId,
                    resolution.getDescription(), resolution.getResolvedAt(),
                    resolution.getFixedVersion(), resolution.getCommitUrl()));
            saved.setStatus(EBugStatus.CLOSED);
            return saved;
        });
        when(userAccountRepository.findById(any())).thenReturn(Optional.of(mock(UserAccount.class)));
        var before = LocalDateTime.now();

        var result = service.close(report.getId(), request);

        assertThat(report.getResolution()).isNotNull();
        assertThat(report.getResolution().getDescription()).isEqualTo(request.description());
        assertThat(report.getResolution().getFixedVersion()).isEqualTo(request.fixedVersion());
        assertThat(report.getResolution().getCommitUrl()).isEqualTo(request.commitUrl());
        assertThat(report.getResolution().getResolvedAt()).isBetween(before, LocalDateTime.now());
        assertThat(report.getStatus()).isEqualTo(EBugStatus.CLOSED);
        assertThat(result.reportId()).isEqualTo(report.getId());
        assertThat(result.message()).isEqualTo("Task has been closed successfully!");
        verify(bugReportRepository).findById(report.getId());
        verify(bugReportRepository).save(report);
    }

    @Test
    void closeThrowsWhenReportDoesNotExist() {
        var reportId = UUID.randomUUID();
        var request = new CloseBugReportRequest("Fixed", "1.1.0", "commit");
        when(bugReportRepository.findById(reportId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.close(reportId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Report with id: %s".formatted(reportId));
        verify(bugReportRepository, never()).save(any());
    }

    @Test
    void closeRejectsAlreadyClosedReport() {
        var report = report(UUID.randomUUID());
        report.setStatus(EBugStatus.CLOSED);
        var request = new CloseBugReportRequest("Fixed", "1.1.0", "commit");
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> service.close(report.getId(), request))
                .isInstanceOf(BusinessRuleConflictException.class)
                .hasMessage("Report is already closed and cannot be reopened.");

        verify(bugReportRepository, never()).save(any());
    }

    @Test
    void updateAssigneeChangesAssigneeAfterValidatingDeveloperRole() {
        var report = report(UUID.randomUUID());
        var assigneeId = UUID.randomUUID();
        var request = new UpdateAssigneeRequest(assigneeId);
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        var developer = mock(UserAccount.class);
        when(developer.getRole()).thenReturn(EUserRole.DEVELOPER);
        when(userAccountRepository.findById(assigneeId)).thenReturn(Optional.of(developer));

        var result = service.updateAssignee(report.getId(), request);

        assertThat(report.getAssigneeId()).isEqualTo(assigneeId);
        assertThat(report.getStatus()).isEqualTo(EBugStatus.ASSIGNED);
        assertUpdateResponse(result, report);
        verify(bugReportRepository).findById(report.getId());
        verify(userAccountRepository).findById(assigneeId);
        verify(bugReportRepository).save(report);
    }

    @Test
    void updateAssigneeKeepsStatusOfReportThatIsNotOpen() {
        var report = report(UUID.randomUUID());
        report.setStatus(EBugStatus.IN_PROGRESS);
        var assigneeId = UUID.randomUUID();
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        var developer = mock(UserAccount.class);
        when(developer.getRole()).thenReturn(EUserRole.DEVELOPER);
        when(userAccountRepository.findById(assigneeId)).thenReturn(Optional.of(developer));

        service.updateAssignee(report.getId(), new UpdateAssigneeRequest(assigneeId));

        assertThat(report.getAssigneeId()).isEqualTo(assigneeId);
        assertThat(report.getStatus()).isEqualTo(EBugStatus.IN_PROGRESS);
    }

    @Test
    void updateSeverityChangesSeverity() {
        var report = report(UUID.randomUUID());
        var request = new UpdateSeverityRequest(EBugSeverity.CRITICAL);
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        var result = service.updateSeverity(report.getId(), request);

        assertThat(report.getSeverity()).isEqualTo(EBugSeverity.CRITICAL);
        assertUpdateResponse(result, report);
        verify(bugReportRepository).findById(report.getId());
    }

    @Test
    void updateStatusChangesStatus() {
        var report = report(UUID.randomUUID());
        report.setStatus(EBugStatus.ASSIGNED);
        var request = new UpdateStatusRequest(EBugStatus.IN_PROGRESS);
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(userAccountRepository.findById(report.getReporterId())).thenReturn(Optional.of(mock(UserAccount.class)));
        when(userAccountRepository.findById(report.getAssigneeId())).thenReturn(Optional.of(mock(UserAccount.class)));

        var result = service.updateStatus(report.getId(), request);

        assertThat(report.getStatus()).isEqualTo(EBugStatus.IN_PROGRESS);
        assertUpdateResponse(result, report);
        verify(bugReportRepository).findById(report.getId());
    }

    @Test
    void updateStatusRejectsDisallowedTransition() {
        var report = report(UUID.randomUUID());
        var request = new UpdateStatusRequest(EBugStatus.REVIEWING);
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> service.updateStatus(report.getId(), request))
                .isInstanceOf(BusinessRuleConflictException.class)
                .hasMessage("Cannot change status from OPEN to REVIEWING.");
        assertThat(report.getStatus()).isEqualTo(EBugStatus.OPEN);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void updateStatusDoesNothingWhenStatusIsUnchanged() {
        var report = report(UUID.randomUUID());
        var request = new UpdateStatusRequest(EBugStatus.OPEN);
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        var result = service.updateStatus(report.getId(), request);

        assertThat(report.getStatus()).isEqualTo(EBugStatus.OPEN);
        assertUpdateResponse(result, report);
        verify(bugReportRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void updateStatusRejectsAssignedStatus() {
        var report = report(UUID.randomUUID());
        var request = new UpdateStatusRequest(EBugStatus.ASSIGNED);
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> service.updateStatus(report.getId(), request))
                .isInstanceOf(BusinessRuleConflictException.class)
                .hasMessage("Use the assignee endpoint to assign a report.");
        assertThat(report.getStatus()).isEqualTo(EBugStatus.OPEN);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void updateStatusRejectsReopeningClosedReport() {
        var report = report(UUID.randomUUID());
        report.setStatus(EBugStatus.CLOSED);
        var request = new UpdateStatusRequest(EBugStatus.OPEN);
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> service.updateStatus(report.getId(), request))
                .isInstanceOf(BusinessRuleConflictException.class)
                .hasMessage("Report is closed and cannot be updated.");
    }

    @Test
    void updateProjectChangesProjectAndRemovesComponentAfterValidatingProject() {
        var report = report(UUID.randomUUID());
        var projectId = UUID.randomUUID();
        var request = new UpdateProjectRequest(projectId);
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(softwareProjectRepository.existsById(projectId)).thenReturn(true);

        var result = service.updateProject(report.getId(), request);

        assertThat(report.getProjectId()).isEqualTo(projectId);
        assertThat(report.getComponentId()).isNull();
        assertUpdateResponse(result, report);
        verify(softwareProjectRepository).existsById(projectId);
    }

    @Test
    void updateProjectKeepsComponentWhenProjectIsUnchanged() {
        var report = report(UUID.randomUUID());
        var componentId = report.getComponentId();
        var request = new UpdateProjectRequest(report.getProjectId());
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(softwareProjectRepository.existsById(report.getProjectId())).thenReturn(true);

        service.updateProject(report.getId(), request);

        assertThat(report.getComponentId()).isEqualTo(componentId);
    }

    @Test
    void updateComponentChangesComponentAfterValidatingItBelongsToTheReportsProject() {
        var report = report(UUID.randomUUID());
        var componentId = UUID.randomUUID();
        var request = new UpdateComponentRequest(componentId);
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        var component = componentOfProject(report.getProjectId());
        when(componentRepository.findById(componentId)).thenReturn(Optional.of(component));

        var result = service.updateComponent(report.getId(), request);

        assertThat(report.getComponentId()).isEqualTo(componentId);
        assertUpdateResponse(result, report);
        verify(componentRepository).findById(componentId);
    }

    @Test
    void updateComponentRemovesComponentWhenIdIsNull() {
        var report = report(UUID.randomUUID());
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        var result = service.updateComponent(report.getId(), new UpdateComponentRequest(null));

        assertThat(report.getComponentId()).isNull();
        assertUpdateResponse(result, report);
        verifyNoInteractions(componentRepository);
    }

    @Test
    void updateComponentThrowsWhenComponentDoesNotExist() {
        var report = report(UUID.randomUUID());
        var componentId = UUID.randomUUID();
        var originalComponentId = report.getComponentId();
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(componentRepository.findById(componentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateComponent(report.getId(), new UpdateComponentRequest(componentId)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Component with id=%s does not exist!".formatted(componentId));
        assertThat(report.getComponentId()).isEqualTo(originalComponentId);
        verify(bugReportRepository, never()).save(any());
    }

    @Test
    void updateComponentThrowsWhenComponentBelongsToAnotherProject() {
        var report = report(UUID.randomUUID());
        var componentId = UUID.randomUUID();
        var originalComponentId = report.getComponentId();
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        var component = componentOfProject(UUID.randomUUID());
        when(componentRepository.findById(componentId)).thenReturn(Optional.of(component));

        assertThatThrownBy(() -> service.updateComponent(report.getId(), new UpdateComponentRequest(componentId)))
                .isInstanceOf(BusinessRuleConflictException.class)
                .hasMessage("Component with id=%s does not belong to project with id=%s."
                        .formatted(componentId, report.getProjectId()));
        assertThat(report.getComponentId()).isEqualTo(originalComponentId);
        verify(bugReportRepository, never()).save(any());
    }

    @Test
    void updateDescriptionChangesDescription() {
        var report = report(UUID.randomUUID());
        var request = new UpdateDescriptionRequest("Updated description");
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        var result = service.updateDescription(report.getId(), request);

        assertThat(report.getDescription()).isEqualTo(request.description());
        assertUpdateResponse(result, report);
    }

    @Test
    void updateStepsToReproduceChangesSteps() {
        var report = report(UUID.randomUUID());
        var request = new UpdateStepsToReproduceRequest("Updated steps");
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        var result = service.updateStepsToReproduce(report.getId(), request);

        assertThat(report.getStepsToReproduce()).isEqualTo(request.stepsToReproduce());
        assertUpdateResponse(result, report);
    }

    @Test
    void updateExpectedBehaviorChangesExpectedBehavior() {
        var report = report(UUID.randomUUID());
        var request = new UpdateExpectedBehaviorRequest("Updated expected behavior");
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        var result = service.updateExpectedBehavior(report.getId(), request);

        assertThat(report.getExpectedBehavior()).isEqualTo(request.expectedBehavior());
        assertUpdateResponse(result, report);
    }

    @Test
    void updateActualBehaviorChangesActualBehavior() {
        var report = report(UUID.randomUUID());
        var request = new UpdateActualBehaviorRequest("Updated actual behavior");
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        var result = service.updateActualBehavior(report.getId(), request);

        assertThat(report.getActualBehavior()).isEqualTo(request.actualBehavior());
        assertUpdateResponse(result, report);
    }

    @Test
    void updateAssigneeThrowsWhenAssigneeDoesNotExist() {
        var report = report(UUID.randomUUID());
        var assigneeId = UUID.randomUUID();
        var request = new UpdateAssigneeRequest(assigneeId);
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(userAccountRepository.findById(assigneeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAssignee(report.getId(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User with id=%s does not exist!".formatted(assigneeId));
        verify(bugReportRepository).findById(report.getId());
        verify(userAccountRepository).findById(assigneeId);
        verify(bugReportRepository, never()).save(any());
    }

    @Test
    void updateAssigneeRejectsNonDeveloper() {
        var report = report(UUID.randomUUID());
        var assigneeId = UUID.randomUUID();
        var account = mock(UserAccount.class);
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(userAccountRepository.findById(assigneeId)).thenReturn(Optional.of(account));
        when(account.getRole()).thenReturn(EUserRole.REPORTER);

        assertThatThrownBy(() -> service.updateAssignee(report.getId(), new UpdateAssigneeRequest(assigneeId)))
                .isInstanceOf(BusinessRuleConflictException.class)
                .hasMessage("Bug reports can only be assigned to developers.");
    }

    @Test
    void updateProjectThrowsWhenProjectDoesNotExist() {
        var report = report(UUID.randomUUID());
        var projectId = UUID.randomUUID();
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(softwareProjectRepository.existsById(projectId)).thenReturn(false);

        assertThatThrownBy(() -> service.updateProject(report.getId(), new UpdateProjectRequest(projectId)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project with id=%s does not exist!".formatted(projectId));
        verify(bugReportRepository, never()).save(any());
    }

    @Test
    void updateSeverityThrowsWhenReportDoesNotExist() {
        var reportId = UUID.randomUUID();
        var request = new UpdateSeverityRequest(EBugSeverity.LOW);
        when(bugReportRepository.findById(reportId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSeverity(reportId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Report with id: %s".formatted(reportId));
        verify(bugReportRepository).findById(reportId);
        verifyNoInteractions(userAccountRepository);
    }

    private static void assertUpdateResponse(
            com.ramy.bugreport.dto.report.UpdateBugReportResponse response,
            BugReport report
    ) {
        assertThat(response.id()).isEqualTo(report.getId());
        assertThat(response.message()).isEqualTo("Bug report updated successfully!");
    }

    private static CreateBugReportRequest createRequest() {
        return new CreateBugReportRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Application crashes",
                "Crash on submit",
                "Open form and submit",
                "Form is saved",
                "Application crashes",
                EBugSeverity.HIGH);
    }

    private static BugReport report(UUID id) {
        var report = BugReport.builder(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Application crashes",
                        EBugSeverity.HIGH)
                .assigneeId(UUID.randomUUID())
                .description("Original description")
                .stepsToReproduce("Original steps")
                .expectedBehavior("Original expected behavior")
                .actualBehavior("Original actual behavior")
                .createdAt(LocalDateTime.now())
                .build();
        report.setId(id);
        return report;
    }

    private void stubUsers(BugReport... reports) {
        var users = java.util.Arrays.stream(reports)
                .flatMap(report -> java.util.stream.Stream.of(
                        user(report.getReporterId(), "Reporter"),
                        user(report.getAssigneeId(), "Assignee")))
                .toList();
        when(userAccountRepository.findAllById(any())).thenReturn(users);
    }

    private static UserAccount user(UUID id, String name) {
        var user = mock(UserAccount.class);
        when(user.getId()).thenReturn(id);
        when(user.getName()).thenReturn(name);
        return user;
    }

    private static UserAccount namedUser(String name) {
        var user = mock(UserAccount.class);
        when(user.getName()).thenReturn(name);
        return user;
    }

    private static SoftwareProject project(String name) {
        var project = mock(SoftwareProject.class);
        when(project.getName()).thenReturn(name);
        return project;
    }

    private static Component componentOfProject(UUID projectId) {
        var component = mock(Component.class);
        when(component.getProjectId()).thenReturn(projectId);
        return component;
    }

    private static Component component(String name) {
        var component = mock(Component.class);
        when(component.getName()).thenReturn(name);
        return component;
    }
}
