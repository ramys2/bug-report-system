package com.ramy.bugreport.dto.report;

import com.ramy.bugreport.domain.EBugStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull EBugStatus status) {
}
