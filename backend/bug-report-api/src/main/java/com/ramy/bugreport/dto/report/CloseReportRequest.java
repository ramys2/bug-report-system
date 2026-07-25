package com.ramy.bugreport.dto.report;

public record CloseReportRequest(
        String description,
        String fixedVersion,
        String commitUrl
) {
}
