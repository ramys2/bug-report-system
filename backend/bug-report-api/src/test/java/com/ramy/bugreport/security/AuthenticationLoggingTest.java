package com.ramy.bugreport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.repository.IUserAccountRepository;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
        SecurityConfig.class,
        CurrentUserAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
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
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        account = new UserAccount(
                UUID.randomUUID(), "Reporter", EMAIL, passwordEncoder.encode(PASSWORD), EUserRole.REPORTER);
        logger = (Logger) LoggerFactory.getLogger(SecurityConfig.class);
        previousLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        logs = new ListAppender<>();
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

        assertLog(Level.WARN, "event=authenticationFailed reason=authenticationFailed");
        assertThat(logs.list.getFirst().getFormattedMessage()).doesNotContain(exceptionMessage);
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
    }
}
