package com.ramy.bugreport.dto.report;

import com.ramy.bugreport.domain.EBugSeverity;

import jakarta.validation.constraints.NotNull;

public record UpdateSeverityRequest(@NotNull EBugSeverity severity) {
}
