package com.ramy.bugreport.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ramy.bugreport.security.UserAccountDetails;

/**
 * Translates exceptions thrown from controllers and services into JSON error responses
 * ({@link ApiErrorResponse}) with the matching HTTP status. Applies to all controllers.
 *
 * <p>Unexpected failures receive a safe generic message. Framework HTTP errors keep their status and headers.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Maps {@link ResourceNotFoundException} to 404 with the exception's message. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(exception.getCode(), exception.getMessage()));
    }
    
    /** Maps business-rule conflicts to 409 with their specific code and English message. */
    @ExceptionHandler(BusinessRuleConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(BusinessRuleConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(exception.getCode(), exception.getMessage()));
    }

    /** Maps an email already in use to 409. */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateEmail(DuplicateEmailException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(exception.getCode(), exception.getMessage()));
    }

    /** Maps database constraint violations (e.g. a foreign key or NOT NULL failure) to 409 with a generic message; details are not exposed. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(ErrorCode.DATA_CONFLICT, "Request conflicts with existing data."));
    }

    /** Maps invalid request values and bodies to 400 without exposing validation details. */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(ErrorCode.INVALID_REQUEST, "Request contains invalid values."));
    }

    /**
     * Maps {@link org.springframework.security.access.AccessDeniedException}, thrown by {@code @PreAuthorize} checks on services, to 403.
     * Denials from the URL rules in {@code SecurityConfig} are answered by {@link com.ramy.bugreport.security.ApiAccessDeniedHandler} instead, with the same body.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication != null && authentication.getPrincipal() instanceof UserAccountDetails user
                ? user.getId() : null;
        logger.warn("event=accessDenied reason=serviceAuthorization userId={}", userId);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse(ErrorCode.ACCESS_DENIED, "Access denied."));
    }

    /**
     * Handles unexpected failures without exposing their details. Spring's HTTP exceptions
     * (for example, an unsupported method or media type) keep their status and response headers.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        HttpStatusCode status = HttpStatus.INTERNAL_SERVER_ERROR;
        HttpHeaders headers = new HttpHeaders();

        if (exception instanceof ErrorResponse errorResponse) {
            status = errorResponse.getStatusCode();
            headers = errorResponse.getHeaders();

            if (!status.is5xxServerError()) {
                HttpStatus knownStatus = HttpStatus.resolve(status.value());
                String message = knownStatus == null ? "Request could not be processed." : knownStatus.getReasonPhrase();
                return ResponseEntity.status(status).headers(headers)
                        .body(new ApiErrorResponse(ErrorCode.INVALID_REQUEST, message));
            }
        }

        // Exception messages can contain sensitive values; record only the exception type.
        if (exception instanceof DataAccessException) {
            logger.error("event=databaseOperationFailed operation=api exceptionType={}",
                    exception.getClass().getSimpleName());
        } else {
            logger.error("Unexpected API error (exceptionType={})", exception.getClass().getName());
        }
        return ResponseEntity.status(status).headers(headers)
                .body(new ApiErrorResponse(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred."));
    }
}
