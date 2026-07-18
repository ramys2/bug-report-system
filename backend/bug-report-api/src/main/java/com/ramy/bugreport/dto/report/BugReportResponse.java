package com.ramy.bugreport.dto.report;

import java.util.List;
import java.util.UUID;

import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.domain.EBugSeverity;
import com.ramy.bugreport.domain.EBugStatus;
import com.ramy.bugreport.domain.Resolution;

public record BugReportResponse(
    UUID id,
    UUID reporterId,
    UUID projectId,
    UUID componentId,
    String title,
    EBugSeverity severity,
    EBugStatus status,
    Resolution resolution
) {

    public static BugReportResponse from(BugReport report) {
        return new BugReportResponse(
            report.getId(),
            report.getReporterId(),
            report.getProjectId(),
            report.getComponentId(),
            report.getTitle(),
            report.getSeverity(),
            report.getStatus(),
            report.getResolution()
        );
    }

}
