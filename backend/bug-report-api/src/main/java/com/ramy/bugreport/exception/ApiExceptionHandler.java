package com.ramy.bugreport.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(exception.getMessage()));
    }
    
    @ExceptionHandler(AdminAccountDeletionException.class)
    public ResponseEntity<ApiErrorResponse> handleAdminAccountDeletion(
    		AdminAccountDeletionException exception
    ) {
    	return ResponseEntity
    			.status(HttpStatus.CONFLICT)
    			.body(new ApiErrorResponse(exception.getMessage()));
    }
}