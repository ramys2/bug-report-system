package com.ramy.bugreport.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions thrown from controllers and services into JSON error responses
 * ({@link ApiErrorResponse}) with the matching HTTP status. Applies to all controllers.
 *
 * <p>Exceptions not listed here are not handled by this class and fall back to Spring's default handling.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Maps {@link ResourceNotFoundException} to 404 with the exception's message. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(exception.getMessage()));
    }
    
    /** Maps {@link DuplicateEmailException} and {@link BusinessRuleConflictException} to 409 with the exception's message. */
    @ExceptionHandler({DuplicateEmailException.class, BusinessRuleConflictException.class})
    public ResponseEntity<ApiErrorResponse> handleConflict(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(exception.getMessage()));
    }

    /** Maps database constraint violations (e.g. a foreign key or NOT NULL failure) to 409 with a generic message; details are not exposed. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse("Request conflicts with existing data."));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    /** Maps failed validation, unreadable JSON, malformed path or query values (e.g. a bad UUID) and missing required query parameters to 400 with a generic message. */
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("Request contains invalid values."));
    }

    /**
     * Maps {@link org.springframework.security.access.AccessDeniedException}, thrown by {@code @PreAuthorize} checks on services, to 403.
     * Denials from the URL rules in {@code SecurityConfig} are answered by {@link com.ramy.bugreport.security.ApiAccessDeniedHandler} instead, with the same body.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse("Access denied."));
    }
}
