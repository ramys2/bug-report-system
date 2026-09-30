package com.ramy.bugreport.openapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.ramy.bugreport.exception.ApiErrorResponse;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/** Documents the 400 error response ({@link ApiErrorResponse} body) on a controller method. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(
        responseCode = "400",
        description = "Validation failed, the JSON is malformed, or a path or query value (for example an id) is malformed.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface BadRequestResponse {
}
