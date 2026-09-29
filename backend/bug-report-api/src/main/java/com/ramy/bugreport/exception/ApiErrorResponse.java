package com.ramy.bugreport.exception;

/**
 * JSON body of every error response: {@code {"message": "..."}}.
 *
 * @param message human-readable description of the error
 */
public record ApiErrorResponse(String message) {
}