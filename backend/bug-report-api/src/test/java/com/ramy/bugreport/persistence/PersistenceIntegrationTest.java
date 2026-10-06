package com.ramy.bugreport.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.domain.Comment;
import com.ramy.bugreport.domain.Component;
import com.ramy.bugreport.domain.EBugSeverity;
import com.ramy.bugreport.domain.EBugStatus;
import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.Resolution;
import com.ramy.bugreport.domain.SoftwareProject;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.dto.component.UpdateComponentDescriptionRequest;
import com.ramy.bugreport.dto.component.UpdateComponentNameRequest;
import com.ramy.bugreport.dto.component.UpdateComponentResponsibleUserRequest;
import com.ramy.bugreport.dto.project.UpdateSoftwareProjectDescriptionRequest;
import com.ramy.bugreport.dto.project.UpdateSoftwareProjectNameRequest;
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
import com.ramy.bugreport.exception.BusinessRuleConflictException;
import com.ramy.bugreport.exception.ResourceNotFoundException;
import com.ramy.bugreport.repository.IBugReportRepository;
import com.ramy.bugreport.repository.ICommentRepository;
import com.ramy.bugreport.repository.IComponentRepository;
import com.ramy.bugreport.repository.IResolutionRepository;
import com.ramy.bugreport.repository.ISoftwareProjectRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;
import com.ramy.bugreport.service.BugReportService;
import com.ramy.bugreport.service.ComponentService;
import com.ramy.bugreport.service.SoftwareProjectService;

// No test transaction: every read must work after the adapter's transaction closes.
@SpringBootTest
@ActiveProfiles("test")
class PersistenceIntegrationTest {
    @Autowired private IUserAccountRepository users;
    @Autowired private ISoftwareProjectRepository projects;
    @Autowired private IComponentRepository components;
    @Autowired private IBugReportRepository reports;
    @Autowired private ICommentRepository comments;
    @Autowired private IResolutionRepository resolutions;
    @Autowired private SoftwareProjectService projectService;
    @Autowired private ComponentService componentService;
    @Autowired private BugReportService reportService;
    @Autowired private JdbcTemplate jdbc;

    private UserAccount reporter;
    private UserAccount developer;
    private SoftwareProject project;
    private Component component;
    private final LocalDateTime now = LocalDateTime.of(2026, 9, 7, 12, 30);

    @BeforeEach
    void setUp() {
        comments.deleteAll();
        reports.deleteAll();
        components.deleteAll();
        projects.deleteAll();
        resolutions.deleteAll();
        users.deleteAll();

        reporter = users.save(new UserAccount("Reporter", "reporter@example.com", "hash", EUserRole.REPORTER));
        developer = users.save(new UserAccount("Developer", "developer@example.com", "hash", EUserRole.DEVELOPER));
        project = projects.save(new SoftwareProject("Project", "Project description"));
        component = components.save(new Component("API", "Component description", developer.getId(), project.getId()));
    }

    @Test
    void componentsAreFoundByProject() {
        var otherProject = projects.save(new SoftwareProject("Other", null));
        components.save(new Component("Other", null, developer.getId(), otherProject.getId()));

        assertThat(components.findByProjectId(project.getId()))
                .usingRecursiveFieldByFieldElementComparator().containsExactly(component);
        assertThat(components.findByProjectId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void accountMappingsAndLookupsPreserveDomainValues() {
        assertThat(reporter.getId()).isNotNull();
        assertThat(users.findById(reporter.getId()).orElseThrow())
                .usingRecursiveComparison().isEqualTo(reporter);
        assertThat(users.findByEmailAddress(reporter.getEmailAddress()).orElseThrow())
                .usingRecursiveComparison().isEqualTo(reporter);
        assertThat(users.existsById(reporter.getId())).isTrue();
        assertThat(users.existsByEmailAddress(reporter.getEmailAddress())).isTrue();
        assertThat(users.findByNameContainingIgnoreCase("PORT"))
                .extracting(UserAccount::getId).containsExactly(reporter.getId());
        assertThat(users.findAllById(List.of(reporter.getId(), developer.getId())))
                .extracting(UserAccount::getId).containsExactlyInAnyOrder(reporter.getId(), developer.getId());
        assertThat(jdbc.queryForObject("SELECT id FROM user_account WHERE email_address = ?",
                String.class, reporter.getEmailAddress())).isEqualTo(reporter.getId().toString());
        assertThat(jdbc.queryForObject("SELECT role FROM user_account WHERE id = ?",
                String.class, developer.getId().toString())).isEqualTo("DEVELOPER");

        reporter.setRole(EUserRole.DEVELOPER);
        users.save(reporter);
        assertThat(users.findById(reporter.getId()).orElseThrow().getRole()).isEqualTo(EUserRole.DEVELOPER);
        assertThat(users.count()).isEqualTo(2);
    }

    @Test
    void projectAndComponentServiceUpdatesArePersisted() {
        projectService.updateName(project.getId(), new UpdateSoftwareProjectNameRequest("Renamed project"));
        projectService.updateDescription(project.getId(), new UpdateSoftwareProjectDescriptionRequest("Long text ".repeat(100)));
        componentService.updateName(component.getId(), new UpdateComponentNameRequest("Renamed component"));
        componentService.updateDescription(component.getId(), new UpdateComponentDescriptionRequest("Updated description"));
        componentService.updateResponsibleUserId(component.getId(), new UpdateComponentResponsibleUserRequest(reporter.getId()));

        var savedProject = projects.findById(project.getId()).orElseThrow();
        assertThat(savedProject.getName()).isEqualTo("Renamed project");
        assertThat(savedProject.getDescription()).isEqualTo("Long text ".repeat(100));
        var savedComponent = components.findById(component.getId()).orElseThrow();
        assertThat(savedComponent.getName()).isEqualTo("Renamed component");
        assertThat(savedComponent.getDescription()).isEqualTo("Updated description");
        assertThat(savedComponent.getResponsibleUserId()).isEqualTo(reporter.getId());
        assertThat(projects.count()).isEqualTo(1);
        assertThat(components.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void bugReportServiceUpdatesArePersisted() {
        var report = reports.save(newReport());
        assertThat(reports.findById(report.getId()).orElseThrow())
                .usingRecursiveComparison().isEqualTo(report);
        var otherProject = projects.save(new SoftwareProject("Other", null));
        var otherComponent = components.save(new Component("Other", null, developer.getId(), otherProject.getId()));

        reportService.updateAssignee(report.getId(), new UpdateAssigneeRequest(developer.getId()));
        reportService.updateSeverity(report.getId(), new UpdateSeverityRequest(EBugSeverity.CRITICAL));
        reportService.updateStatus(report.getId(), new UpdateStatusRequest(EBugStatus.IN_PROGRESS));
        reportService.updateProject(report.getId(), new UpdateProjectRequest(otherProject.getId()));
        assertThat(reports.findById(report.getId()).orElseThrow().getComponentId()).isNull();
        reportService.updateComponent(report.getId(), new UpdateComponentRequest(otherComponent.getId()));
        reportService.updateDescription(report.getId(), new UpdateDescriptionRequest("Updated description"));
        reportService.updateStepsToReproduce(report.getId(), new UpdateStepsToReproduceRequest("Updated steps"));
        reportService.updateExpectedBehavior(report.getId(), new UpdateExpectedBehaviorRequest("Updated expected"));
        reportService.updateActualBehavior(report.getId(), new UpdateActualBehaviorRequest("Updated actual"));

        var saved = reports.findById(report.getId()).orElseThrow();
        assertThat(saved.getAssigneeId()).isEqualTo(developer.getId());
        assertThat(saved.getSeverity()).isEqualTo(EBugSeverity.CRITICAL);
        assertThat(saved.getStatus()).isEqualTo(EBugStatus.IN_PROGRESS);
        assertThat(saved.getProjectId()).isEqualTo(otherProject.getId());
        assertThat(saved.getComponentId()).isEqualTo(otherComponent.getId());
        assertThat(saved.getDescription()).isEqualTo("Updated description");
        assertThat(saved.getStepsToReproduce()).isEqualTo("Updated steps");
        assertThat(saved.getExpectedBehavior()).isEqualTo("Updated expected");
        assertThat(saved.getActualBehavior()).isEqualTo("Updated actual");
        assertThat(saved.getCreatedAt()).isEqualTo(now);
        assertThat(saved.getUpdatedAt()).isAfter(now);
        assertThat(reports.findByReporterId(reporter.getId())).extracting(BugReport::getId).containsExactly(report.getId());
        assertThat(reports.findByAssigneeId(developer.getId())).extracting(BugReport::getId).containsExactly(report.getId());
        assertThat(reports.existsByAssigneeIdAndStatusNot(developer.getId(), EBugStatus.CLOSED)).isTrue();
        assertThat(reports.count()).isEqualTo(1);
    }

    @Test
    void createSetsCreatedAtAndUpdatedAtInRealDatabase() {
        var request = new CreateBugReportRequest(
                null, project.getId(), component.getId(), "New bug", null, null, null, null, EBugSeverity.LOW);

        var response = reportService.create(reporter.getId(), request);

        var saved = reports.findById(response.id()).orElseThrow();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    void createWithUnknownReporterFailsWithNotFound() {
        var request = new CreateBugReportRequest(
                null, project.getId(), component.getId(), "New bug", null, null, null, null, EBugSeverity.LOW);

        assertThatThrownBy(() -> reportService.create(UUID.randomUUID(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("does not exist");
        assertThat(reports.count()).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void closingReportReturnsSavedResolutionAndLoadsItOutsideTransaction() {
        var report = newReport();
        report.setAssigneeId(developer.getId());
        var saved = reports.save(report);
        var request = new CloseBugReportRequest("Fixed", "1.2.3", "https://example.com/commit/1");

        var response = reportService.close(saved.getId(), request);

        var closed = reports.findById(saved.getId()).orElseThrow();
        assertThat(closed.getStatus()).isEqualTo(EBugStatus.CLOSED);
        assertThat(response.reportId()).isEqualTo(saved.getId());
        assertThat(closed.getResolution().getDescription()).isEqualTo("Fixed");
        assertThat(closed.getResolution().getFixedVersion()).isEqualTo("1.2.3");
        assertThat(closed.getResolution().getCommitUrl()).isEqualTo(request.commitUrl());
        assertThat(closed.getResolution().getResolvedAt()).isNotNull();
        assertThat(reports.findByAssigneeId(developer.getId()).getFirst().getResolution().getDescription()).isEqualTo("Fixed");
        assertThat(reports.existsByAssigneeIdAndStatusNot(developer.getId(), EBugStatus.CLOSED)).isFalse();
        assertThat(resolutions.findById(closed.getResolution().getId()).orElseThrow())
                .usingRecursiveComparison().isEqualTo(closed.getResolution());
        assertThatThrownBy(() -> reportService.close(saved.getId(), request))
                .isInstanceOf(BusinessRuleConflictException.class);
        assertThat(resolutions.count()).isEqualTo(1);
    }

    @Test
    void resolutionCascadeAndOrphanRemovalSurviveDomainMapping() {
        var report = newReport();
        report.setResolution(new Resolution("First fix", now, "1.0", null));
        var saved = reports.save(report);
        UUID firstResolutionId = saved.getResolution().getId();
        assertThat(firstResolutionId).isNotNull();
        assertThat(reports.findAll().getFirst()).usingRecursiveComparison().isEqualTo(saved);

        saved = reports.save(saved);
        assertThat(saved.getResolution().getId()).isEqualTo(firstResolutionId);
        assertThat(resolutions.count()).isEqualTo(1);

        saved.setResolution(new Resolution("Second fix", now, "1.1", null));
        saved = reports.save(saved);
        assertThat(saved.getResolution().getId()).isNotEqualTo(firstResolutionId);
        assertThat(resolutions.findById(firstResolutionId)).isEmpty();
        assertThat(resolutions.count()).isEqualTo(1);

        saved.setResolution(null);
        saved = reports.save(saved);
        assertThat(reports.findById(saved.getId()).orElseThrow().getResolution()).isNull();
        assertThat(resolutions.count()).isZero();

        saved.setResolution(new Resolution("Final fix", now, null, null));
        reports.save(saved);
        reports.deleteAll();
        assertThat(resolutions.count()).isZero();
    }

    @Test
    void commentsRoundTripAndDeleteThroughDomainRepository() {
        var report = reports.save(newReport());
        var comment = comments.save(new Comment(report.getId(), reporter.getId(), "Comment ".repeat(100), now));

        assertThat(comment.getId()).isNotNull();
        assertThat(comments.findById(comment.getId()).orElseThrow())
                .usingRecursiveComparison().isEqualTo(comment);
        assertThat(comments.findByBugReportId(report.getId()).getFirst())
                .usingRecursiveComparison().isEqualTo(comment);
        assertThat(comments.findByBugReportId(UUID.randomUUID())).isEmpty();

        comments.delete(comment);
        assertThat(comments.findById(comment.getId())).isEmpty();
        assertThat(comments.count()).isZero();
    }

    private BugReport newReport() {
        return BugReport.builder(reporter.getId(), project.getId(), component.getId(), "Bug title", EBugSeverity.HIGH)
                .description("Description ".repeat(100))
                .stepsToReproduce("Steps")
                .expectedBehavior("Expected")
                .actualBehavior("Actual")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
