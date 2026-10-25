package com.ramy.bugreport.dto.support;

/** Confirmation returned with HTTP 201 after creating a support request. */
public record CreateSupportRequestResponse(
        String code,
        String message
) {
}
