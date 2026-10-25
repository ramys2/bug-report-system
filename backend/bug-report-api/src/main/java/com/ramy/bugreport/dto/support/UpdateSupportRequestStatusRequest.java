package com.ramy.bugreport.dto.support;

import com.ramy.bugreport.domain.ESupportRequestStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** The service permits only APPROVED or REJECTED as a resolution. */
public record UpdateSupportRequestStatusRequest(
        @Schema(description = "Resolution status: APPROVED or REJECTED.", example = "APPROVED")
        @NotNull ESupportRequestStatus status
) {
}
