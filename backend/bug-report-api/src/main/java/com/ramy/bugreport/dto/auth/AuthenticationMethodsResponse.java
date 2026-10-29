package com.ramy.bugreport.dto.auth;

import org.jspecify.annotations.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;

/** Public capabilities only; never returns OAuth credentials. */
public record AuthenticationMethodsResponse(boolean googleEnabled,
        @Schema(types = {"string", "null"}, description = "Application-relative Google login path; null when Google login is disabled.")
        @Nullable String googleLoginUrl) {
}
