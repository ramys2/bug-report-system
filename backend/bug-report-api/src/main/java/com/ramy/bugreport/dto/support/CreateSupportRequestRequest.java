package com.ramy.bugreport.dto.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ramy.bugreport.domain.ESupportRequestType;
import com.ramy.bugreport.domain.EUserRole;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Creation input only: identity, status and timestamps are always supplied by the server. */
@Schema(description = "Request body of POST /api/support. ROLE_CHANGE requires requestedRole; OTHER requires a description and no requestedRole.")
public record CreateSupportRequestRequest(
        @NotNull ESupportRequestType type,
        EUserRole requestedRole,
        @Size(max = 10000) String description
) {
    /** Validates the fields that depend on the selected request type. */
    @JsonIgnore
    @AssertTrue(message = "Provide a target role for ROLE_CHANGE, or a non-blank description without a role for OTHER.")
    public boolean isDetailsValid() {
        if (type == ESupportRequestType.ROLE_CHANGE) {
            return requestedRole != null;
        }
        if (type == ESupportRequestType.OTHER) {
            return requestedRole == null && description != null && !description.isBlank();
        }
        // @NotNull reports a missing type separately.
        return true;
    }
}
