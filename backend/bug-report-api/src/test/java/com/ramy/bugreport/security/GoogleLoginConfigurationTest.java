package com.ramy.bugreport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GoogleLoginConfigurationTest {
    @Test
    void absentOrPartialCredentialsDoNotBuildRegistration() {
        for (String[] pair : new String[][] {{"", ""}, {"id", ""}, {"", "secret"}, {" ", "secret"}}) {
            var configuration = new GoogleLoginConfiguration(pair[0], pair[1], "http://localhost:5173",
                    "/api/auth/oauth2/authorization", "/api/auth/oauth2/code");
            assertThat(configuration.isConfigured()).isFalse();
            assertThat(configuration.getGoogleLoginUrl()).isNull();
            assertThat(configuration.registrations().findByRegistrationId("google")).isNull();
        }
    }

    @Test
    void completeCredentialsUseConfiguredOriginAndCallback() {
        var configuration = new GoogleLoginConfiguration("test-id", "test-secret", "https://bugs.example.test:9443/",
                "/api/sign-in/google-start", "/api/sign-in/google-callback");
        assertThat(configuration.isConfigured()).isTrue();
        var google = configuration.registrations().findByRegistrationId("google");
        assertThat(configuration.getGoogleLoginUrl()).isEqualTo("/api/sign-in/google-start/google");
        assertThat(google.getScopes()).containsExactlyInAnyOrder("openid", "email");
        assertThat(google.getProviderDetails().getIssuerUri()).isEqualTo("https://accounts.google.com");
        assertThat(google.getRedirectUri()).isEqualTo("https://bugs.example.test:9443/api/sign-in/google-callback/google");
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "//example.test", "https://user:secret@example.test",
            "https://example.test/path", "https://example.test?query=yes", "https://example.test#fragment",
            "https://example.test:70000", "https://example.test/\\evil", ""})
    void invalidOriginsAreRejectedWithoutEchoingTheirValues(String origin) {
        assertThatThrownBy(() -> new GoogleLoginConfiguration("id", "secret", origin,
                "/api/start", "/api/callback"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Google frontend-origin must be an HTTP(S) origin without credentials, query or fragment.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://evil.test/api/start", "//evil.test/start", "/outside/api", "/api/start/",
            "/api/../start", "/api/start?query=yes", "/api/start#fragment", "/api/start/*", "/api//start", ""})
    void invalidAuthorizationAndCallbackBasesAreRejected(String path) {
        assertThatThrownBy(() -> new GoogleLoginConfiguration("id", "secret", "http://localhost:5173",
                path, "/api/callback")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GoogleLoginConfiguration("id", "secret", "http://localhost:5173",
                "/api/start", path)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void authorizationAndCallbackBasesMustDiffer() {
        assertThatThrownBy(() -> new GoogleLoginConfiguration("id", "secret", "http://localhost:5173",
                "/api/same", "/api/same")).isInstanceOf(IllegalArgumentException.class);
    }
}
