package com.ramy.bugreport.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Provides the CSRF token the frontend must send with state-changing requests. */
@Tag(name = "Authentication")
@RestController
public class CsrfController {

    /**
     * {@code GET /api/csrf}: returns the CSRF token for the current session.
     *
     * <p>Access: public.
     *
     * @return 200 with {@code {headerName, parameterName, token}}; the token goes into the {@code X-CSRF-TOKEN} header
     */
    @Operation(summary = "Get a CSRF token", description = "Returns the CSRF token for the current session and sets the session cookie. Send the token in the `X-CSRF-TOKEN` header with every POST, PATCH and DELETE request. Access: public.")
    @GetMapping("/api/csrf")
    public CsrfToken csrf(@Parameter(hidden = true) CsrfToken csrfToken) {
        return csrfToken;
    }
}