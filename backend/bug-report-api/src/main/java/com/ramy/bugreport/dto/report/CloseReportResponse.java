package com.ramy.bugreport.dto.report;

import java.util.UUID;

public record CloseReportResponse(UUID reportId, String message) {

}
