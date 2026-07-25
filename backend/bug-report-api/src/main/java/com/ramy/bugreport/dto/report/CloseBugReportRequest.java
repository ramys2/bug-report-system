package com.ramy.bugreport.dto.report;

public record CloseBugReportRequest(
        String description,
        String fixedVersion,
        String commitUrl
) {
}
