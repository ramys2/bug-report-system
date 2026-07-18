package com.ramy.bugreport.dto.report;

import java.util.UUID;

public record CreateBugReportResponse(UUID id, String message) {
}
