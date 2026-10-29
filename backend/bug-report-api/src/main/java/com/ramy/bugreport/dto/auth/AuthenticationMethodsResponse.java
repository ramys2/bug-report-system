package com.ramy.bugreport.dto.auth;

/** Public capabilities only; never returns OAuth credentials. */
public record AuthenticationMethodsResponse(boolean googleEnabled) {
}
