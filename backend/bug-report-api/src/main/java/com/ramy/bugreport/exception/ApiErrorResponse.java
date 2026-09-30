package com.ramy.bugreport.exception;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JSON body of every error response: {@code {"message": "..."}}.
 *
 * @param message human-readable description of the error
 */
@Schema(description = "JSON body of every error response.")
public record ApiErrorResponse(
        @Schema(description = "Human-readable description of the error.", example = "Request contains invalid values.", requiredMode = Schema.RequiredMode.REQUIRED)
        String message
) {
}