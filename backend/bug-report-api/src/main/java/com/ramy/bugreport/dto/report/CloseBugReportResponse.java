package com.ramy.bugreport.dto.report;

import java.util.UUID;

public record CloseBugReportResponse(UUID reportId, String message) {

}
