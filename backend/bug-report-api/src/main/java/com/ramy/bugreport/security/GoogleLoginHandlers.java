package com.ramy.bugreport.security;

import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

import com.ramy.bugreport.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Redirects browser logins without exposing provider errors or accepting external return URLs. */
public class GoogleLoginHandlers implements AuthenticationSuccessHandler, AuthenticationFailureHandler {
    private static final Logger logger = LoggerFactory.getLogger(GoogleLoginHandlers.class);
    private static final Set<String> PUBLIC_ERRORS = Set.of(
            ErrorCode.GOOGLE_EMAIL_CONFLICT.name(), ErrorCode.GOOGLE_EMAIL_NOT_VERIFIED.name());
    private final String frontendOrigin;

    public GoogleLoginHandlers(String frontendOrigin) {
        this.frontendOrigin = frontendOrigin;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        String target = clearTemporaryState(request);
        var user = (UserAccountDetails) authentication.getPrincipal();
        logger.info("event=googleAuthenticationSucceeded userId={}", user.getId());
        response.sendRedirect(frontendOrigin + target);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        clearTemporaryState(request);
        String code = ErrorCode.GOOGLE_LOGIN_FAILED.name();
        if (exception instanceof OAuth2AuthenticationException oauth
                && PUBLIC_ERRORS.contains(oauth.getError().getErrorCode())) {
            code = oauth.getError().getErrorCode();
        }
        logger.warn("event=googleAuthenticationFailed reason={}", code);
        response.sendRedirect(frontendOrigin + "/login?oauthError=" + code);
    }

    private String clearTemporaryState(HttpServletRequest request) {
        var session = request.getSession(false);
        Object target = session == null ? null : session.getAttribute(GoogleAuthorizationRequestResolver.RETURN_TO);
        if (session != null) {
            session.removeAttribute(GoogleAuthorizationRequestResolver.RETURN_TO);
        }
        new HttpSessionRequestCache().removeRequest(request, null);
        return GoogleAuthorizationRequestResolver.safeReturnTo(target instanceof String path ? path : null);
    }
}
