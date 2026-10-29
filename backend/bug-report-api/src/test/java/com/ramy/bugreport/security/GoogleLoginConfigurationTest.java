package com.ramy.bugreport.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GoogleLoginConfigurationTest {
    @Test
    void absentOrPartialCredentialsDoNotBuildRegistration() {
        for (String[] pair : new String[][] {{"", ""}, {"id", ""}, {"", "secret"}, {" ", "secret"}}) {
            var configuration = new GoogleLoginConfiguration(pair[0], pair[1]);
            assertThat(configuration.isConfigured()).isFalse();
            assertThat(configuration.registrations().findByRegistrationId("google")).isNull();
        }
    }

    @Test
    void completeCredentialsBuildOnlyGoogleWithMinimalScopesAndFixedCallback() {
        var configuration = new GoogleLoginConfiguration("test-id", "test-secret");
        assertThat(configuration.isConfigured()).isTrue();
        var google = configuration.registrations().findByRegistrationId("google");
        assertThat(google.getScopes()).containsExactlyInAnyOrder("openid", "email");
        assertThat(google.getProviderDetails().getIssuerUri()).isEqualTo("https://accounts.google.com");
        assertThat(google.getRedirectUri()).isEqualTo("http://localhost:5173/api/auth/oauth2/code/google");
    }
}
