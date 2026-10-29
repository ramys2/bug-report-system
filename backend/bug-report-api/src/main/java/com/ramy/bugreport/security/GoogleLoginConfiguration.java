package com.ramy.bugreport.security;

import java.net.URI;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.stereotype.Component;

/** Optional Google registration. No registration is built when either credential is absent. */
@Component
public class GoogleLoginConfiguration {
    private final ClientRegistrationRepository registrations;
    private final boolean configured;
    private final String frontendOrigin;
    private final String authorizationBase;
    private final String callbackBase;

    public GoogleLoginConfiguration(
            @Value("${app.auth.google.client-id:}") String clientId,
            @Value("${app.auth.google.client-secret:}") String clientSecret,
            @Value("${app.auth.google.frontend-origin}") String frontendOrigin,
            @Value("${app.auth.google.authorization-base}") String authorizationBase,
            @Value("${app.auth.google.callback-base}") String callbackBase) {
        this.frontendOrigin = validateOrigin(frontendOrigin);
        this.authorizationBase = validateBase(authorizationBase);
        this.callbackBase = validateBase(callbackBase);
        if (authorizationBase.equals(callbackBase)) {
            throw new IllegalArgumentException("Google authorization-base and callback-base must differ.");
        }
        configured = !clientId.isBlank() && !clientSecret.isBlank();
        if (configured) {
            var google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(clientId).clientSecret(clientSecret)
                    .scope("openid", "email")
                    .redirectUri(this.frontendOrigin + this.callbackBase + "/google")
                    .build();
            registrations = new InMemoryClientRegistrationRepository(google);
        } else {
            registrations = registrationId -> null;
        }
    }

    public boolean isConfigured() {
        return configured;
    }

    public @Nullable String getGoogleLoginUrl() {
        return configured ? authorizationBase + "/google" : null;
    }

    String getFrontendOrigin() {
        return frontendOrigin;
    }

    String getAuthorizationBase() {
        return authorizationBase;
    }

    String getCallbackBase() {
        return callbackBase;
    }

    ClientRegistrationRepository registrations() {
        return registrations;
    }

    private static String validateOrigin(String value) {
        try {
            URI origin = URI.create(value);
            if (("http".equalsIgnoreCase(origin.getScheme()) || "https".equalsIgnoreCase(origin.getScheme()))
                    && origin.getHost() != null && origin.getRawUserInfo() == null
                    && origin.getRawQuery() == null && origin.getRawFragment() == null
                    && (origin.getRawPath().isEmpty() || origin.getRawPath().equals("/"))
                    && (origin.getPort() == -1 || origin.getPort() >= 1 && origin.getPort() <= 65535)) {
                return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
            }
        } catch (IllegalArgumentException exception) {
            // Configuration errors must not echo submitted values into startup logs.
        }
        throw new IllegalArgumentException("Google frontend-origin must be an HTTP(S) origin without credentials, query or fragment.");
    }

    private static String validateBase(String value) {
        if (!value.matches("/api(?:/[A-Za-z0-9_-]+)+")) {
            throw new IllegalArgumentException("Google endpoint bases must be paths under /api with no trailing slash.");
        }
        return value;
    }
}
