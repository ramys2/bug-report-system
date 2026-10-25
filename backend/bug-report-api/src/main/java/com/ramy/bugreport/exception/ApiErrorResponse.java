package com.ramy.bugreport.exception;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JSON body of API error responses: {@code {"code": "...", "message": "..."}}.
 *
 * @param code stable identifier used by clients to translate the error
 * @param message human-readable description of the error
 */
@Schema(description = "JSON body of every error response.")
public record ApiErrorResponse(
        @Schema(description = "Stable error code for client-side localization.", example = "INVALID_REQUEST", requiredMode = Schema.RequiredMode.REQUIRED)
        ErrorCode code,
        @Schema(description = "Human-readable description of the error.", example = "Request contains invalid values.", requiredMode = Schema.RequiredMode.REQUIRED)
        String message
) {
}
