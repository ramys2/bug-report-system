package com.ramy.bugreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.domain.EBugSeverity;
import com.ramy.bugreport.domain.EBugStatus;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.dto.report.CloseBugReportRequest;
import com.ramy.bugreport.dto.report.CreateBugReportRequest;
import com.ramy.bugreport.dto.report.UpdateBugReportRequest;
import com.ramy.bugreport.exception.ResourceNotFoundException;
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

    private BugReportService service;

    @BeforeEach
    void setUp() {
        service = new BugReportService(
                bugReportRepository,
                userAccountRepository,
                softwareProjectRepository,
                componentRepository);
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
                .containsExactly("Application crashes", "Reporter", "Assignee", "open", "high", "13-07-2026 12:05");
        verify(bugReportRepository).findAll();
    }

    @Test
    void getReportReturnsMappedReport() {
        var report = report(UUID.randomUUID());
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        var result = service.getReport(report.getId());

        assertThat(result.id()).isEqualTo(report.getId());
        assertThat(result.title()).isEqualTo(report.getTitle());
        verify(bugReportRepository).findById(report.getId());
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
        when(componentRepository.existsById(request.componentId())).thenReturn(true);
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
        verify(componentRepository).existsById(request.componentId());
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
        assertThat(savedReport.getStatus()).isEqualTo(EBugStatus.OPEN);
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
        when(componentRepository.existsById(request.componentId())).thenReturn(false);

        assertThatThrownBy(() -> service.create(reporterId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Component with id=%s does not exist!".formatted(request.componentId()));
        verify(userAccountRepository).existsById(reporterId);
        verify(softwareProjectRepository).existsById(request.projectId());
        verify(componentRepository).existsById(request.componentId());
        verify(bugReportRepository, never()).save(any());
    }

    @Test
    void closeAddsResolutionAndSavesReport() {
        var report = report(UUID.randomUUID());
        var request = new CloseBugReportRequest("Fixed null handling", "1.1.0", "https://example.com/commit/1");
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        var before = LocalDateTime.now();

        var result = service.close(report.getId(), request);

        assertThat(report.getResolution()).isNotNull();
        assertThat(report.getResolution().getDescription()).isEqualTo(request.description());
        assertThat(report.getResolution().getFixedVersion()).isEqualTo(request.fixedVersion());
        assertThat(report.getResolution().getCommitUrl()).isEqualTo(request.commitUrl());
        assertThat(report.getResolution().getResolvedAt()).isBetween(before, LocalDateTime.now());
        assertThat(result.reportId()).isEqualTo(report.getResolution().getId());
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
    void updateAppliesEveryProvidedField() {
        var report = report(UUID.randomUUID());
        var assigneeId = UUID.randomUUID();
        var request = new UpdateBugReportRequest(
                assigneeId,
                "Updated description",
                "Updated steps",
                "Updated expected behavior",
                "Updated actual behavior",
                EBugStatus.IN_PROGRESS);
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(userAccountRepository.existsById(assigneeId)).thenReturn(true);

        var result = service.update(report.getId(), request);

        assertThat(report.getAssigneeId()).isEqualTo(assigneeId);
        assertThat(report.getDescription()).isEqualTo(request.description());
        assertThat(report.getStepsToReproduce()).isEqualTo(request.stepsToReproduce());
        assertThat(report.getExpectedBehavior()).isEqualTo(request.expectedBehavior());
        assertThat(report.getActualBehavior()).isEqualTo(request.actualBehavior());
        assertThat(report.getStatus()).isEqualTo(request.bugStatus());
        assertThat(result.id()).isEqualTo(report.getId());
        assertThat(result.message()).isEqualTo("Bug report updated successfully!");
        verify(bugReportRepository).findById(report.getId());
        verify(userAccountRepository).existsById(assigneeId);
        verify(bugReportRepository, never()).save(any());
    }

    @Test
    void updateLeavesFieldsUntouchedWhenValuesAreNull() {
        var report = report(UUID.randomUUID());
        var originalDescription = report.getDescription();
        var request = new UpdateBugReportRequest(null, null, null, null, null, null);
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        service.update(report.getId(), request);

        assertThat(report.getDescription()).isEqualTo(originalDescription);
        assertThat(report.getStatus()).isEqualTo(EBugStatus.OPEN);
        verify(bugReportRepository).findById(report.getId());
        verifyNoInteractions(userAccountRepository);
        verify(bugReportRepository, never()).save(any());
    }

    @Test
    void updateThrowsWhenAssigneeDoesNotExist() {
        var report = report(UUID.randomUUID());
        var assigneeId = UUID.randomUUID();
        var request = new UpdateBugReportRequest(assigneeId, null, null, null, null, null);
        when(bugReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(userAccountRepository.existsById(assigneeId)).thenReturn(false);

        assertThatThrownBy(() -> service.update(report.getId(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User with id=%s does not exist!".formatted(assigneeId));
        verify(bugReportRepository).findById(report.getId());
        verify(userAccountRepository).existsById(assigneeId);
        verify(bugReportRepository, never()).save(any());
    }

    @Test
    void updateThrowsWhenReportDoesNotExist() {
        var reportId = UUID.randomUUID();
        var request = new UpdateBugReportRequest(null, null, null, null, null, null);
        when(bugReportRepository.findById(reportId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(reportId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Report with id: %s".formatted(reportId));
        verify(bugReportRepository).findById(reportId);
        verifyNoInteractions(userAccountRepository);
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
}
