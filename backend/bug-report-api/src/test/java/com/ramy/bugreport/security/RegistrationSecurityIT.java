package com.ramy.bugreport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.repository.IBugReportRepository;
import com.ramy.bugreport.repository.ICommentRepository;
import com.ramy.bugreport.repository.IComponentRepository;
import com.ramy.bugreport.repository.IResolutionRepository;
import com.ramy.bugreport.repository.ISoftwareProjectRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;

import tools.jackson.databind.ObjectMapper;

/** Real security filters, BCrypt, repositories and MariaDB; no enclosing test transaction. */
@SpringBootTest
@WebAppConfiguration
@ActiveProfiles("test")
class RegistrationSecurityIT {
    @Autowired private WebApplicationContext context;
    @Autowired private IUserAccountRepository users;
    @Autowired private IBugReportRepository reports;
    @Autowired private ICommentRepository comments;
    @Autowired private IComponentRepository components;
    @Autowired private ISoftwareProjectRepository projects;
    @Autowired private IResolutionRepository resolutions;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        comments.deleteAll();
        reports.deleteAll();
        components.deleteAll();
        projects.deleteAll();
        resolutions.deleteAll();
        users.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void absentGoogleCredentialsHideTheMethodAndLeaveTheStartEndpointInactive() throws Exception {
        mockMvc.perform(get("/api/auth/methods"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.googleEnabled").value(false));
        mockMvc.perform(get("/api/auth/oauth2/authorization/google"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void guestCanRegisterWithFetchedCsrfTokenThenLogInUsingExistingSessionAuthentication() throws Exception {
        var session = new MockHttpSession();
        var csrfResponse = mockMvc.perform(get("/api/csrf").session(session))
                .andExpect(status().isOk()).andReturn().getResponse();
        var token = objectMapper.readTree(csrfResponse.getContentAsString());

        mockMvc.perform(post("/api/accounts").session(session)
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .contentType("application/json")
                        .content(body(" Ramy ", "Ramy@Example.COM", "Password123!")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(unauthenticated());

        var saved = users.findByEmailAddress("ramy@example.com").orElseThrow();
        assertThat(saved.getName()).isEqualTo("Ramy");
        assertThat(saved.getRole()).isEqualTo(EUserRole.REPORTER);
        assertThat(saved.getPasswordHash()).isNotEqualTo("Password123!");
        assertThat(passwordEncoder.matches("Password123!", saved.getPasswordHash())).isTrue();

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").session(session).with(csrf())
                        .param("username", "ramy@example.com").param("password", "Password123!"))
                .andExpect(status().isOk())
                .andExpect(authenticated().withRoles("REPORTER"));
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.role").value("REPORTER"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "DEVELOPER"})
    void clientCannotAssignPrivilegedRoleOrIdentity(String role) throws Exception {
        mockMvc.perform(post("/api/accounts").with(csrf()).contentType("application/json")
                        .content("""
                                {"name":"Ramy","email":"ramy@example.com","password":"secret",
                                 "role":"%s","id":"00000000-0000-0000-0000-000000000001"}
                                """.formatted(role)))
                .andExpect(status().isCreated())
                .andExpect(unauthenticated());

        var saved = users.findByEmailAddress("ramy@example.com").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(EUserRole.REPORTER);
        assertThat(saved.getId().toString()).isNotEqualTo("00000000-0000-0000-0000-000000000001");
    }

    @Test
    void registrationStillRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/api/accounts").contentType("application/json")
                        .content(body("Ramy", "ramy@example.com", "secret")))
                .andExpect(status().isForbidden());
        assertThat(users.count()).isZero();
    }

    @Test
    void duplicateNormalizedEmailReturnsConflictWithoutCreatingAnAccount() throws Exception {
        register("Ramy", "ramy@example.com");

        mockMvc.perform(post("/api/accounts").with(csrf()).contentType("application/json")
                        .content(body("Other", "RAMY@EXAMPLE.COM", "secret")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void duplicateTrimmedUsernameReturnsConflictWithoutCreatingAnAccount() throws Exception {
        register("Ramy", "ramy@example.com");

        mockMvc.perform(post("/api/accounts").with(csrf()).contentType("application/json")
                        .content(body(" Ramy ", "other@example.com", "secret")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_USERNAME"));
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void databaseEnforcesUsernameUniquenessEvenWhenServiceCheckIsBypassed() throws Exception {
        register("Ramy", "ramy@example.com");

        assertThatThrownBy(() -> users.save(new UserAccount("Ramy", "other@example.com", "hash", EUserRole.REPORTER)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void wrongPasswordStillFailsLogin() throws Exception {
        register("Ramy", "ramy@example.com");

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", "ramy@example.com").param("password", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(unauthenticated());
    }

    @Test
    void bcryptLimitCountsUtf8BytesAndAllowsExactly72Bytes() throws Exception {
        String password = "ě".repeat(36);
        mockMvc.perform(post("/api/accounts").with(csrf()).contentType("application/json")
                        .content(body("Ramy", "ramy@example.com", password)))
                .andExpect(status().isCreated());
        assertThat(passwordEncoder.matches(password,
                users.findByEmailAddress("ramy@example.com").orElseThrow().getPasswordHash())).isTrue();

        mockMvc.perform(post("/api/accounts").with(csrf()).contentType("application/json")
                        .content(body("Other", "other@example.com", password + "a")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        assertThat(users.count()).isEqualTo(1);
    }

    private void register(String name, String email) throws Exception {
        mockMvc.perform(post("/api/accounts").with(csrf()).contentType("application/json")
                        .content(body(name, email, "Password123!")))
                .andExpect(status().isCreated());
    }

    private String body(String name, String email, String password) {
        return objectMapper.writeValueAsString(java.util.Map.of("name", name, "email", email, "password", password));
    }
}
