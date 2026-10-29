package com.ramy.bugreport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.repository.IUserAccountRepository;
import com.ramy.bugreport.exception.ApiExceptionHandler;
import com.ramy.bugreport.logging.RequestLoggingFilter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
        SecurityConfig.class,
        GoogleLoginConfiguration.class,
        CurrentUserAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        ApiExceptionHandler.class,
        RequestLoggingFilter.class,
        AuthenticationLoggingTest.TestConfig.class
})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class AuthenticationLoggingTest {

    private static final String EMAIL = "reporter@example.com";
    private static final String PASSWORD = "Password123!";

    @Autowired private WebApplicationContext context;
    @Autowired private IUserAccountRepository userAccountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private UserAccount account;
    private Logger logger;
    private Level previousLevel;
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void setUp() {
        reset(userAccountRepository);
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean(RequestLoggingFilter.class)).apply(springSecurity()).build();
        account = new UserAccount(
                UUID.randomUUID(), "Reporter", EMAIL, passwordEncoder.encode(PASSWORD), EUserRole.REPORTER);
        logger = (Logger) LoggerFactory.getLogger("com.ramy.bugreport");
        previousLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        logs = new ListAppender<>() {
            @Override
            protected void append(ILoggingEvent event) {
                event.prepareForDeferredProcessing();
                super.append(event);
            }
        };
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
        logger.setLevel(previousLevel);
        logs.stop();
    }

    @Test
    void successfulLoginLogsOnlyUserIdAndKeepsEmpty200Response() throws Exception {
        when(userAccountRepository.findByEmailAddress(EMAIL)).thenReturn(Optional.of(account));

        mockMvc.perform(formLogin("/api/auth/login").user(EMAIL).password(PASSWORD))
                .andExpect(status().isOk())
                .andExpect(content().string(""))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(authenticated().withUsername(EMAIL));

        assertLog(Level.INFO, "event=authenticationSucceeded userId=" + account.getId());
        assertThat(logs.list.getFirst().getArgumentArray()).containsExactly(account.getId());
        assertThat(UUID.fromString(logs.list.getFirst().getMDCPropertyMap().get("requestId"))).isNotNull();
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void wrongPasswordLogsWarnAndKeepsGeneric401Response() throws Exception {
        when(userAccountRepository.findByEmailAddress(EMAIL)).thenReturn(Optional.of(account));

        assertRejectedLogin(EMAIL, "WrongPassword123!");

        assertLog(Level.WARN, "event=authenticationFailed reason=invalidCredentials");
    }

    @Test
    void unknownUserLogsSameFailureWithoutEchoingSubmittedInput() throws Exception {
        String submittedEmail = "unknown@example.com\r\nFORGED_LOG_ENTRY";
        when(userAccountRepository.findByEmailAddress("unknown@example.com\r\nforged_log_entry"))
                .thenReturn(Optional.empty());

        assertRejectedLogin(submittedEmail, PASSWORD);

        assertLog(Level.WARN, "event=authenticationFailed reason=invalidCredentials");
        assertThat(logs.list.getFirst().getFormattedMessage())
                .doesNotContain(submittedEmail, "FORGED_LOG_ENTRY", "\r", "\n");
    }

    @Test
    void authenticationServiceFailureKeepsApplicationEntryFreeOfExceptionDetails() throws Exception {
        String exceptionMessage = "Account lookup failed with private diagnostic details";
        when(userAccountRepository.findByEmailAddress(EMAIL))
                .thenThrow(new IllegalStateException(exceptionMessage));

        assertRejectedLogin(EMAIL, PASSWORD);

        assertThat(logs.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.ERROR, Level.WARN);
        assertThat(logs.list.getFirst().getFormattedMessage())
                .isEqualTo("event=authenticationServiceFailed exceptionType=InternalAuthenticationServiceException");
        assertThat(logs.list.getLast().getFormattedMessage())
                .isEqualTo("event=authenticationFailed reason=authenticationFailed");
        assertThat(logs.list).allSatisfy(entry -> {
            assertThat(entry.getFormattedMessage()).doesNotContain(exceptionMessage, EMAIL, PASSWORD);
            assertThat(entry.getThrowableProxy()).isNull();
        });
    }

    @Test
    void logoutLogsUserIdAndInvalidatesAuthentication() throws Exception {
        when(userAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        mockMvc.perform(post("/api/auth/logout").with(authentication(authenticationForAccount())).with(csrf()))
                .andExpect(status().isOk()).andExpect(unauthenticated());
        assertLog(Level.INFO, "event=logoutSucceeded userId=" + account.getId());
    }

    @Test
    void anonymousLogoutDoesNotRequireAnAccountToLog() throws Exception {
        mockMvc.perform(post("/api/auth/logout").with(csrf())).andExpect(status().isOk());
        assertLog(Level.INFO, "event=logoutSucceeded userId=null");
    }

    @Test
    void urlAuthorizationLogsExactlyOneDenial() throws Exception {
        when(userAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        mockMvc.perform(get("/api/accounts").with(authentication(authenticationForAccount())))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        assertLog(Level.WARN, "event=accessDenied reason=urlAuthorization userId=" + account.getId());
    }

    @Test
    void csrfRejectionIsDistinctFromAuthorizationDenials() throws Exception {
        when(userAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        mockMvc.perform(post("/api/comments").with(authentication(authenticationForAccount())))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        assertLog(Level.WARN, "event=accessDenied reason=csrfRejected userId=" + account.getId());
    }

    @Test
    void methodSecurityLogsExactlyOneServiceDenial() throws Exception {
        when(userAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        mockMvc.perform(get("/api/logging-test").with(authentication(authenticationForAccount())))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        assertLog(Level.WARN, "event=accessDenied reason=serviceAuthorization userId=" + account.getId());
    }

    @Test
    void removedAccountIsAnonymousForCurrentRequestAndLogsOnlyAtDebug() throws Exception {
        when(userAccountRepository.findById(account.getId())).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/logging-test").with(authentication(authenticationForAccount())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        assertLog(Level.DEBUG, "event=staleAuthenticationCleared userId=" + account.getId());
    }

    private UsernamePasswordAuthenticationToken authenticationForAccount() {
        var details = new UserAccountDetails(account);
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    private void assertRejectedLogin(String email, String password) throws Exception {
        mockMvc.perform(formLogin("/api/auth/login").user(email).password(password))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password."))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(unauthenticated());
    }

    private void assertLog(Level level, String message) {
        assertThat(logs.list).singleElement().satisfies(entry -> {
            assertThat(entry.getLevel()).isEqualTo(level);
            assertThat(entry.getFormattedMessage()).isEqualTo(message)
                    .doesNotContain(EMAIL, PASSWORD, "WrongPassword123!", account.getPasswordHash(), account.getName());
            assertThat(entry.getThrowableProxy()).isNull();
        });
    }

    /** Real security filters with a mocked repository, without database or messaging infrastructure. */
    @Configuration
    @EnableWebMvc
    static class TestConfig {
        @Bean
        IUserAccountRepository userAccountRepository() {
            return mock(IUserAccountRepository.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        DeniedService deniedService() {
            return new DeniedService();
        }

        @Bean
        TestController testController(DeniedService service) {
            return new TestController(service);
        }
    }

    static class DeniedService {
        @PreAuthorize("denyAll()")
        public void execute() { }
    }

    @RestController
    static class TestController {
        private final DeniedService service;

        TestController(DeniedService service) {
            this.service = service;
        }

        @GetMapping("/api/logging-test")
        public void denied() {
            service.execute();
        }
    }
}
