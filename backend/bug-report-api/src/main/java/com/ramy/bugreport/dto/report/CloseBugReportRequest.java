package com.ramy.bugreport.dto.report;

import jakarta.validation.constraints.NotBlank;

public record CloseBugReportRequest(
        @NotBlank String description,
        String fixedVersion,
        String commitUrl
) {
}
