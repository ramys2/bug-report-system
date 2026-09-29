package com.ramy.bugreport.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Provides the CSRF token the frontend must send with state-changing requests. */
@RestController
public class CsrfController {

    /**
     * {@code GET /api/csrf}: returns the CSRF token for the current session.
     *
     * <p>Access: public.
     *
     * @return 200 with {@code {headerName, parameterName, token}}; the token goes into the {@code X-CSRF-TOKEN} header
     */
    @GetMapping("/api/csrf")
    public CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }
}