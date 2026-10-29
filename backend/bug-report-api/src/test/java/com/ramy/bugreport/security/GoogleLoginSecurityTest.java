package com.ramy.bugreport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.util.UriComponentsBuilder;

import com.ramy.bugreport.controller.AuthController;
import com.ramy.bugreport.controller.CsrfController;
import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.domain.Comment;
import com.ramy.bugreport.component.BugReportAuthorizer;
import com.ramy.bugreport.component.CommentAuthorizer;
import com.ramy.bugreport.component.UserAccountAuthorizer;
import com.ramy.bugreport.exception.BusinessRuleConflictException;
import com.ramy.bugreport.exception.ErrorCode;
import com.ramy.bugreport.repository.IUserAccountRepository;
import com.ramy.bugreport.repository.IBugReportRepository;
import com.ramy.bugreport.repository.ICommentRepository;
import com.ramy.bugreport.service.GoogleAccountService;

import tools.jackson.databind.ObjectMapper;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

/** Real authorization/callback filters and nonce/session handling; only provider I/O and storage are stubbed. */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {SecurityConfig.class, GoogleLoginConfiguration.class,
        CurrentUserAuthenticationFilter.class, ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class,
        AuthController.class, CsrfController.class, GoogleLoginSecurityTest.TestConfig.class})
@TestPropertySource(properties = {"app.cors.allowed-origins=http://localhost:5173",
        "app.auth.google.client-id=test-client", "app.auth.google.client-secret=test-secret"})
class GoogleLoginSecurityTest {
    @Autowired private WebApplicationContext context;
    @Autowired private IUserAccountRepository users;
    @Autowired private GoogleAccountService accounts;
    @Autowired private JwtDecoder decoder;
    @Autowired private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenClient;
    @Autowired private PasswordEncoder passwordEncoder;

    private MockMvc mvc;
    private UserAccount account;
    private boolean verified;
    private String tokenNonce;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        reset(users, accounts, decoder, tokenClient);
        account = new UserAccount(UUID.randomUUID(), "google-user", "google@example.com", null,
                EUserRole.REPORTER, "subject-1");
        verified = true;
        tokenNonce = null;
        when(users.findById(account.getId())).thenAnswer(invocation -> Optional.of(account));
        when(accounts.resolve("subject-1", "google@example.com")).thenAnswer(invocation -> account);
        when(tokenClient.getTokenResponse(any())).thenReturn(OAuth2AccessTokenResponse.withToken("test-access")
                .tokenType(OAuth2AccessToken.TokenType.BEARER).expiresIn(300)
                .scopes(Set.of("openid", "email")).additionalParameters(Map.of("id_token", "test-id-token")).build());
        when(decoder.decode("test-id-token")).thenAnswer(invocation -> Jwt.withTokenValue("test-id-token")
                .header("alg", "RS256").issuer("https://accounts.google.com").subject("subject-1")
                .audience(List.of("test-client")).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
                .claim("nonce", tokenNonce).claim("email", "google@example.com").claim("email_verified", verified).build());
    }

    @Test
    void callbackCreatesCompatibleSessionRestoresRouteRefreshesRoleAndLogsOutLocally() throws Exception {
        var flow = begin("/reports/00000000-0000-0000-0000-000000000001?tab=comments#latest");
        String oldSessionId = flow.session.getId();
        callback(flow).andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:5173/reports/00000000-0000-0000-0000-000000000001?tab=comments#latest"))
                .andExpect(authenticated().withRoles("REPORTER"));
        assertThat(flow.session.getId()).isNotEqualTo(oldSessionId);
        assertThat(flow.session.getAttribute(GoogleAuthorizationRequestResolver.RETURN_TO)).isNull();
        mvc.perform(get("/api/auth/me").session(flow.session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(account.getId().toString()))
                .andExpect(jsonPath("$.name").value("google-user"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.googleSubject").doesNotExist());
        account.setRole(EUserRole.DEVELOPER);
        mvc.perform(get("/api/auth/me").session(flow.session)).andExpect(jsonPath("$.role").value("DEVELOPER"));
        var storedContext = (SecurityContext) flow.session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(storedContext.getAuthentication()).isInstanceOf(OAuth2AuthenticationToken.class);
        assertThat(storedContext.getAuthentication().getPrincipal()).isInstanceOf(GoogleUserAccountDetails.class);
        mvc.perform(get("/api/accounts").session(flow.session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").session(flow.session)).andExpect(status().isForbidden());
        var csrfResult = mvc.perform(get("/api/csrf").session(flow.session)).andReturn();
        var csrfToken = new ObjectMapper().readTree(csrfResult.getResponse().getContentAsString());
        mvc.perform(post("/api/auth/logout").session(flow.session)
                        .header(csrfToken.get("headerName").asText(), csrfToken.get("token").asText()))
                .andExpect(status().isOk()).andExpect(unauthenticated()).andExpect(header().doesNotExist("Location"));
        assertThat(flow.session.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        verify(tokenClient, times(1)).getTokenResponse(any());
    }

    @Test
    void removedAccountCannotUseGoogleSession() throws Exception {
        var flow = begin("/");
        callback(flow).andExpect(authenticated());
        when(users.findById(account.getId())).thenReturn(Optional.empty());
        mvc.perform(get("/api/auth/me").session(flow.session)).andExpect(status().isUnauthorized());
    }

    @Test
    void googlePrincipalWorksWithExistingOwnershipAndSelfRoleChecks() throws Exception {
        var flow = begin("/");
        callback(flow).andExpect(authenticated());
        var security = (SecurityContext) flow.session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        var authentication = security.getAuthentication();
        UUID reportId = UUID.randomUUID();
        var reports = mock(IBugReportRepository.class);
        var report = mock(BugReport.class);
        when(reports.findById(reportId)).thenReturn(Optional.of(report));
        when(report.getReporterId()).thenReturn(account.getId());
        var reportAuthorizer = new BugReportAuthorizer(reports);
        assertThat(reportAuthorizer.canUpdate(reportId, authentication)).isTrue();
        when(report.getReporterId()).thenReturn(UUID.randomUUID());
        assertThat(reportAuthorizer.canUpdate(reportId, authentication)).isFalse();
        when(report.getAssigneeId()).thenReturn(account.getId());
        assertThat(reportAuthorizer.canUpdate(reportId, authentication)).isTrue();

        UUID commentId = UUID.randomUUID();
        var comments = mock(ICommentRepository.class);
        var comment = mock(Comment.class);
        when(comments.findById(commentId)).thenReturn(Optional.of(comment));
        when(comment.getAuthorId()).thenReturn(account.getId());
        var commentAuthorizer = new CommentAuthorizer(comments);
        assertThat(commentAuthorizer.canDelete(commentId, authentication)).isTrue();
        when(comment.getAuthorId()).thenReturn(UUID.randomUUID());
        assertThat(commentAuthorizer.canDelete(commentId, authentication)).isFalse();
        var accountAuthorizer = new UserAccountAuthorizer();
        assertThat(accountAuthorizer.canUpdateRole(account.getId(), authentication)).isFalse();
        assertThat(accountAuthorizer.canUpdateRole(UUID.randomUUID(), authentication)).isTrue();
    }

    @Test
    void accountStorageFailureIsGenericAndLogsNoExceptionDetails() throws Exception {
        var flow = begin("/");
        when(accounts.resolve(any(), any())).thenThrow(new IllegalStateException("private database diagnostic"));
        var logger = (Logger) LoggerFactory.getLogger(GoogleOidcUserService.class);
        var logs = new ListAppender<ILoggingEvent>();
        logs.start();
        logger.addAppender(logs);
        try {
            callback(flow).andExpect(redirectedUrl("http://localhost:5173/login?oauthError=GOOGLE_LOGIN_FAILED"))
                    .andExpect(unauthenticated());
            assertThat(logs.list).singleElement().satisfies(event -> {
                assertThat(event.getFormattedMessage()).isEqualTo(
                        "event=googleAccountResolutionFailed exceptionType=IllegalStateException");
                assertThat(event.getThrowableProxy()).isNull();
            });
        } finally {
            logger.detachAppender(logs);
            logs.stop();
        }
    }

    @Test
    void callbackRejectsInvalidStateBeforeContactingTokenEndpoint() throws Exception {
        var flow = begin("/");
        mvc.perform(get(GoogleLoginConfiguration.CALLBACK_BASE + "/google").session(flow.session)
                        .param("code", "private-code").param("state", "wrong-state"))
                .andExpect(redirectedUrl("http://localhost:5173/login?oauthError=GOOGLE_LOGIN_FAILED"))
                .andExpect(unauthenticated());
        verifyNoInteractions(tokenClient, accounts);
    }

    @Test
    void decoderRejectionDoesNotExposePrivateDiagnostics() throws Exception {
        var flow = begin("/");
        when(decoder.decode(any())).thenThrow(new BadJwtException("private token or secret diagnostic"));
        var logger = (Logger) LoggerFactory.getLogger(GoogleLoginHandlers.class);
        var logs = new ListAppender<ILoggingEvent>();
        logs.start();
        logger.addAppender(logs);
        try {
            callback(flow).andExpect(redirectedUrl("http://localhost:5173/login?oauthError=GOOGLE_LOGIN_FAILED"))
                    .andExpect(unauthenticated());
            assertThat(logs.list).singleElement().satisfies(event -> {
                assertThat(event.getFormattedMessage()).isEqualTo("event=googleAuthenticationFailed reason=GOOGLE_LOGIN_FAILED");
                assertThat(event.getThrowableProxy()).isNull();
            });
        } finally {
            logger.detachAppender(logs);
            logs.stop();
        }
        verifyNoInteractions(accounts);
    }

    @Test
    void providerTokenExchangeFailureIsGenericAndNeverCreatesAnAccount() throws Exception {
        var flow = begin("/");
        when(tokenClient.getTokenResponse(any())).thenThrow(new org.springframework.security.oauth2.core.OAuth2AuthorizationException(
                new org.springframework.security.oauth2.core.OAuth2Error("invalid_grant", "private diagnostic", null)));
        callback(flow).andExpect(redirectedUrl("http://localhost:5173/login?oauthError=GOOGLE_LOGIN_FAILED"))
                .andExpect(unauthenticated());
        verifyNoInteractions(accounts);
    }

    @Test
    void callbackCannotBeReplayedToResolveTheAccountTwice() throws Exception {
        var flow = begin("/");
        callback(flow).andExpect(authenticated());
        callback(flow).andExpect(redirectedUrl("http://localhost:5173/login?oauthError=GOOGLE_LOGIN_FAILED"));
        verify(accounts, times(1)).resolve("subject-1", "google@example.com");
    }

    @Test
    void nonceMismatchRejectsLoginBeforeAccountResolution() throws Exception {
        var flow = begin("/");
        tokenNonce = "wrong-nonce";
        callback(flow).andExpect(redirectedUrl("http://localhost:5173/login?oauthError=GOOGLE_LOGIN_FAILED"));
        verifyNoInteractions(accounts);
    }

    @Test
    void googleDenialIsGenericAndClearsReturnTarget() throws Exception {
        var flow = begin("/admin/users");
        mvc.perform(get(GoogleLoginConfiguration.CALLBACK_BASE + "/google").session(flow.session)
                        .param("error", "access_denied").param("error_description", "private details")
                        .param("state", flow.authorization.getState()))
                .andExpect(redirectedUrl("http://localhost:5173/login?oauthError=GOOGLE_LOGIN_FAILED"));
        assertThat(flow.session.getAttribute(GoogleAuthorizationRequestResolver.RETURN_TO)).isNull();
        verifyNoInteractions(tokenClient, accounts);
    }

    @Test
    void unverifiedEmailCannotResolveOrCreateAccount() throws Exception {
        var flow = begin("/");
        verified = false;
        callback(flow).andExpect(redirectedUrl("http://localhost:5173/login?oauthError=GOOGLE_EMAIL_NOT_VERIFIED"))
                .andExpect(unauthenticated());
        verifyNoInteractions(accounts);
    }

    @Test
    void accountConflictRedirectsWithOnlyTheApprovedErrorCode() throws Exception {
        var flow = begin("/");
        when(accounts.resolve(any(), any())).thenThrow(new BusinessRuleConflictException(
                ErrorCode.GOOGLE_EMAIL_CONFLICT, "private account details"));
        callback(flow).andExpect(redirectedUrl("http://localhost:5173/login?oauthError=GOOGLE_EMAIL_CONFLICT"))
                .andExpect(unauthenticated());
    }

    @Test
    void passwordLoginStillWorksWhenGoogleIsConfiguredButRejectsPasswordlessAccounts() throws Exception {
        when(users.findByEmailAddress(account.getEmailAddress())).thenReturn(Optional.of(account));
        mvc.perform(post("/api/auth/login").with(csrf()).param("username", account.getEmailAddress())
                        .param("password", "anything"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        account = new UserAccount(account.getId(), "local-user", account.getEmailAddress(),
                passwordEncoder.encode("local-password"), EUserRole.DEVELOPER, "subject-1");
        when(users.findByEmailAddress(account.getEmailAddress())).thenReturn(Optional.of(account));
        mvc.perform(post("/api/auth/login").with(csrf()).param("username", account.getEmailAddress())
                        .param("password", "local-password"))
                .andExpect(status().isOk()).andExpect(authenticated().withRoles("DEVELOPER"));
        mvc.perform(post("/api/auth/login").param("username", account.getEmailAddress()).param("password", "local-password"))
                .andExpect(status().isForbidden());
    }

    @Test
    void methodsExposeOnlyCapabilityAndAnonymousApiStillReturns401() throws Exception {
        mvc.perform(get("/api/auth/methods")).andExpect(status().isOk())
                .andExpect(content().json("{\"googleEnabled\":true}"));
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://evil.example/", "//evil.example/", "/\\evil.example", "/api/auth/logout",
            "/login", "/%2fevil.example", "/admin/users%0d%0aLocation:evil", "/admin/../users"})
    void unsafeReturnTargetsFallBackToHome(String target) throws Exception {
        var flow = begin(target);
        callback(flow).andExpect(redirectedUrl("http://localhost:5173/"));
    }

    private Flow begin(String target) throws Exception {
        var session = new MockHttpSession();
        var result = mvc.perform(get(GoogleLoginConfiguration.AUTHORIZATION_BASE + "/google")
                        .session(session).param("returnTo", target))
                .andExpect(status().isFound()).andReturn();
        String location = result.getResponse().getRedirectedUrl();
        assertThat(URI.create(location).getHost()).isEqualTo("accounts.google.com");
        var params = UriComponentsBuilder.fromUriString(location).build().getQueryParams();
        assertThat(params.getFirst("redirect_uri")).isEqualTo(
                "http://localhost:5173/api/auth/oauth2/code/google");
        var request = new MockHttpServletRequest();
        request.setSession(session);
        request.setParameter("state", URLDecoder.decode(params.getFirst("state"), StandardCharsets.UTF_8));
        var authorization = new HttpSessionOAuth2AuthorizationRequestRepository().loadAuthorizationRequest(request);
        tokenNonce = (String) authorization.getAdditionalParameters().get("nonce");
        return new Flow(session, authorization);
    }

    private org.springframework.test.web.servlet.ResultActions callback(Flow flow) throws Exception {
        return mvc.perform(get(GoogleLoginConfiguration.CALLBACK_BASE + "/google").session(flow.session)
                .param("state", flow.authorization.getState()).param("code", "test-code"));
    }

    private record Flow(MockHttpSession session, OAuth2AuthorizationRequest authorization) { }

    @Configuration
    @EnableWebMvc
    static class TestConfig {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
        @Bean IUserAccountRepository users() { return mock(IUserAccountRepository.class); }
        @Bean GoogleAccountService accounts() { return mock(GoogleAccountService.class); }
        @Bean JwtDecoder decoder() { return mock(JwtDecoder.class); }
        @Bean JwtDecoderFactory<ClientRegistration> jwtDecoders(JwtDecoder decoder) { return registration -> decoder; }
        @Bean OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenClient() {
            return mock(OAuth2AccessTokenResponseClient.class);
        }
        @Bean GoogleOidcUserService googleUsers(GoogleAccountService accounts) {
            return new GoogleOidcUserService(accounts,
                    request -> new DefaultOidcUser(List.of(), request.getIdToken()));
        }
    }
}
