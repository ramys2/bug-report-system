package com.ramy.bugreport.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.domain.EBugSeverity;
import com.ramy.bugreport.domain.EBugStatus;
import com.ramy.bugreport.persistence.entity.BugReportEntity;
import com.ramy.bugreport.persistence.entity.ResolutionEntity;

class BugReportMapperTest {

    @Test
    void newReportDefaultsToOpenWithoutIdOrResolution() {
        var report = BugReport.builder(UUID.randomUUID(), UUID.randomUUID(), null,
                "Application crashes", EBugSeverity.HIGH).build();

        assertThat(report.getId()).isNull();
        assertThat(report.getStatus()).isEqualTo(EBugStatus.OPEN);
        assertThat(report.getResolution()).isNull();
    }

    @Test
    void builderRejectsExplicitNullStatus() {
        var builder = BugReport.builder(UUID.randomUUID(), UUID.randomUUID(), null,
                "Application crashes", EBugSeverity.HIGH);

        assertThatThrownBy(() -> builder.status(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("status is required");
    }

    @Test
    void restoresClosedReportWithAllStoredValues() {
        var createdAt = LocalDateTime.of(2026, 7, 13, 12, 5);
        var resolvedAt = createdAt.plusDays(1);
        var resolution = new ResolutionEntity();
        resolution.setId(UUID.randomUUID());
        resolution.setDescription("Fixed the crash");
        resolution.setResolvedAt(resolvedAt);
        resolution.setFixedVersion("1.0.1");
        resolution.setCommitUrl("https://example.com/commit/123");

        var entity = new BugReportEntity();
        entity.setId(UUID.randomUUID());
        entity.setReporterId(UUID.randomUUID());
        entity.setAssigneeId(UUID.randomUUID());
        entity.setProjectId(UUID.randomUUID());
        entity.setComponentId(UUID.randomUUID());
        entity.setTitle("Application crashes");
        entity.setDescription("Crashes when saving a report");
        entity.setStepsToReproduce("Create and save a report");
        entity.setExpectedBehavior("Report is saved");
        entity.setActualBehavior("Application crashes");
        entity.setSeverity(EBugSeverity.HIGH);
        entity.setStatus(EBugStatus.CLOSED);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(resolvedAt);
        entity.setResolution(resolution);

        var report = BugReportMapper.toDomain(entity);

        assertThat(report).usingRecursiveComparison().isEqualTo(entity);
    }
}
