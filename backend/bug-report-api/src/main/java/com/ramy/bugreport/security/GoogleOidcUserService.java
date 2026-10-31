package com.ramy.bugreport.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import com.ramy.bugreport.exception.BusinessRuleConflictException;
import com.ramy.bugreport.exception.ErrorCode;
import com.ramy.bugreport.service.GoogleAccountService;

/** Lets Spring handle OIDC, then resolves the verified identity to an application account. */
@Component
public class GoogleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {
    private static final Logger logger = LoggerFactory.getLogger(GoogleOidcUserService.class);
    private final GoogleAccountService accounts;
    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;

    @Autowired
    public GoogleOidcUserService(GoogleAccountService accounts) {
        this(accounts, new OidcUserService());
    }

    GoogleOidcUserService(GoogleAccountService accounts, OAuth2UserService<OidcUserRequest, OidcUser> delegate) {
        this.accounts = accounts;
        this.delegate = delegate;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) {
        try {
            OidcUser google = delegate.loadUser(request);
            if (!Boolean.TRUE.equals(google.getEmailVerified())) {
                throw failure(ErrorCode.GOOGLE_EMAIL_NOT_VERIFIED);
            }
            return new GoogleUserAccountDetails(accounts.resolve(google.getSubject(), google.getEmail()), google);
        } catch (BusinessRuleConflictException exception) {
            throw failure(exception.getCode());
        } catch (OAuth2AuthenticationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            // Neither provider exceptions nor SQL exception messages are safe to log.
            logger.error("event=googleAccountResolutionFailed exceptionType={}", exception.getClass().getSimpleName());
            throw failure(ErrorCode.GOOGLE_LOGIN_FAILED);
        }
    }

    private OAuth2AuthenticationException failure(ErrorCode code) {
        return new OAuth2AuthenticationException(new OAuth2Error(code.name()));
    }
}
