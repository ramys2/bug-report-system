package com.ramy.bugreport.openapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.ramy.bugreport.exception.ApiErrorResponse;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/** Documents the 404 error response ({@link ApiErrorResponse} body) on a controller method. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(
        responseCode = "404",
        description = "A referenced resource does not exist.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface NotFoundResponse {
}
