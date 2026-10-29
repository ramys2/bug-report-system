package com.ramy.bugreport.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.stereotype.Component;

/** Optional Google registration. No registration is built when either credential is absent. */
@Component
public class GoogleLoginConfiguration {
    public static final String FRONTEND_ORIGIN = "http://localhost:5173";
    public static final String AUTHORIZATION_BASE = "/api/auth/oauth2/authorization";
    public static final String CALLBACK_BASE = "/api/auth/oauth2/code";

    private final ClientRegistrationRepository registrations;
    private final boolean configured;

    public GoogleLoginConfiguration(
            @Value("${app.auth.google.client-id:}") String clientId,
            @Value("${app.auth.google.client-secret:}") String clientSecret) {
        configured = !clientId.isBlank() && !clientSecret.isBlank();
        if (configured) {
            var google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(clientId).clientSecret(clientSecret)
                    .scope("openid", "email")
                    .redirectUri(FRONTEND_ORIGIN + CALLBACK_BASE + "/google")
                    .build();
            registrations = new InMemoryClientRegistrationRepository(google);
        } else {
            registrations = registrationId -> null;
        }
    }

    public boolean isConfigured() {
        return configured;
    }

    ClientRegistrationRepository registrations() {
        return registrations;
    }
}
