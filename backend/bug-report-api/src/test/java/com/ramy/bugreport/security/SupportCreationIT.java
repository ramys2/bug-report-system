package com.ramy.bugreport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ramy.bugreport.domain.ESupportRequestStatus;
import com.ramy.bugreport.domain.ESupportRequestType;
import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.SupportRequest;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.dto.support.CreateSupportRequestRequest;
import com.ramy.bugreport.repository.IBugReportRepository;
import com.ramy.bugreport.repository.ICommentRepository;
import com.ramy.bugreport.repository.IComponentRepository;
import com.ramy.bugreport.repository.IResolutionRepository;
import com.ramy.bugreport.repository.ISoftwareProjectRepository;
import com.ramy.bugreport.repository.ISupportRequestRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;
import com.ramy.bugreport.service.SupportService;

import tools.jackson.databind.ObjectMapper;

/** Real HTTP security, services, Flyway and MariaDB; each save commits outside any test transaction. */
@SpringBootTest
@WebAppConfiguration
@ActiveProfiles("test")
class SupportCreationIT {
    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ISupportRequestRepository requests;
    @Autowired private IUserAccountRepository users;
    @Autowired private IBugReportRepository reports;
    @Autowired private ICommentRepository comments;
    @Autowired private IComponentRepository components;
    @Autowired private ISoftwareProjectRepository projects;
    @Autowired private IResolutionRepository resolutions;
    @Autowired private SupportService service;

    private MockMvc mockMvc;
    private UserAccount reporter;
    private UserAccount developer;
    private UserAccount admin;

    @BeforeEach
    void setUp() {
        requests.deleteAll();
        comments.deleteAll();
        reports.deleteAll();
        components.deleteAll();
        projects.deleteAll();
        resolutions.deleteAll();
        users.deleteAll();
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        reporter = saveUser("Reporter", EUserRole.REPORTER);
        developer = saveUser("Developer", EUserRole.DEVELOPER);
        admin = saveUser("Admin", EUserRole.ADMIN);
    }

    @AfterEach
    void cleanUp() {
        // Existing integration tests delete users, so do not leave support foreign keys behind.
        requests.deleteAll();
        SecurityContextHolder.clearContext();
    }

    @Test
    void reporterCanPersistPendingDeveloperRequestWithoutChangingTheirRole() throws Exception {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        mockMvc.perform(roleRequest(EUserRole.DEVELOPER)
                        .with(authentication(authenticationFor(reporter))).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUPPORT_REQUEST_CREATED"))
                .andExpect(jsonPath("$.message").value("Support request created successfully."))
                .andExpect(jsonPath("$.length()").value(2));

        var saved = onlySavedRequest();
        assertThat(saved.getRequesterId()).isEqualTo(reporter.getId());
        assertThat(saved.getType()).isEqualTo(ESupportRequestType.ROLE_CHANGE);
        assertThat(saved.getRequestedRole()).isEqualTo(EUserRole.DEVELOPER);
        assertThat(saved.getStatus()).isEqualTo(ESupportRequestStatus.PENDING);
        assertThat(saved.getDescription()).isNull();
        assertThat(saved.getResolvedAt()).isNull();
        assertThat(saved.getCreatedAt()).isBetween(before, LocalDateTime.now());
        assertThat(users.findById(reporter.getId()).orElseThrow().getRole()).isEqualTo(EUserRole.REPORTER);
    }

    @ParameterizedTest
    @EnumSource(value = EUserRole.class, names = {"REPORTER", "DEVELOPER"})
    void bothNonAdministratorRolesCanCreateMultipleGeneralRequests(EUserRole role) throws Exception {
        var account = role == EUserRole.REPORTER ? reporter : developer;
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(generalRequest(" Please help ").with(authentication(authenticationFor(account))).with(csrf()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUPPORT_REQUEST_CREATED"));
        }
        assertThat(requests.count()).isEqualTo(2);
        assertThat(jdbc.queryForList("SELECT description FROM support_request", String.class))
                .containsExactly("Please help", "Please help");
        assertThat(jdbc.queryForList("SELECT requested_role FROM support_request", String.class)).containsOnlyNulls();
        assertThat(jdbc.queryForList("SELECT status FROM support_request", String.class)).containsOnly("PENDING");
        assertThat(users.findById(account.getId()).orElseThrow().getRole()).isEqualTo(role);
    }

    @Test
    void unauthenticatedCreationRequiresLogin() throws Exception {
        mockMvc.perform(generalRequest("Help").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        assertThat(requests.count()).isZero();
    }

    @Test
    void administratorCannotCreateSupportRequest() throws Exception {
        mockMvc.perform(generalRequest("Help").with(authentication(authenticationFor(admin))).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(requests.count()).isZero();
    }

    @Test
    void csrfProtectionIsPreserved() throws Exception {
        mockMvc.perform(generalRequest("Help").with(authentication(authenticationFor(reporter))))
                .andExpect(status().isForbidden());
        assertThat(requests.count()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"REPORTER,REPORTER", "REPORTER,ADMIN", "DEVELOPER,REPORTER", "DEVELOPER,DEVELOPER", "DEVELOPER,ADMIN"})
    void currentLowerAndAdministratorRolesAreRejected(EUserRole currentRole, EUserRole requestedRole) throws Exception {
        var account = currentRole == EUserRole.REPORTER ? reporter : developer;
        mockMvc.perform(roleRequest(requestedRole).with(authentication(authenticationFor(account))).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUPPORT_ROLE_NOT_ALLOWED"));
        assertThat(requests.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\n\t", "\u2003"})
    void blankGeneralDescriptionsCannotBeSaved(String description) throws Exception {
        mockMvc.perform(generalRequest(description).with(authentication(authenticationFor(reporter))).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        assertThat(requests.count()).isZero();
    }

    @Test
    void roleChangeRequiresRequestedRole() throws Exception {
        mockMvc.perform(post("/api/support").with(authentication(authenticationFor(reporter))).with(csrf())
                        .contentType("application/json").content("{\"type\":\"ROLE_CHANGE\"}"))
                .andExpect(status().isBadRequest());
        assertThat(requests.count()).isZero();
    }

    @Test
    void identityStatusIdAndTimestampsCannotBeForged() throws Exception {
        UUID forgedId = UUID.randomUUID();
        mockMvc.perform(post("/api/support").with(authentication(authenticationFor(reporter))).with(csrf())
                        .contentType("application/json").content(objectMapper.writeValueAsString(Map.of(
                                "type", "OTHER", "description", "Help", "requesterId", developer.getId(),
                                "status", "APPROVED", "id", forgedId,
                                "createdAt", "2000-01-01T00:00:00", "resolvedAt", "2000-01-02T00:00:00"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUPPORT_REQUEST_CREATED"))
                .andExpect(jsonPath("$.length()").value(2));
        var saved = onlySavedRequest();
        assertThat(saved.getId()).isNotEqualTo(forgedId);
        assertThat(saved.getRequesterId()).isEqualTo(reporter.getId());
        assertThat(saved.getStatus()).isEqualTo(ESupportRequestStatus.PENDING);
        assertThat(saved.getCreatedAt()).isAfter(LocalDateTime.of(2000, 1, 2, 0, 0));
        assertThat(saved.getResolvedAt()).isNull();
    }

    @Test
    void duplicatePendingRoleRequestReturnsConflict() throws Exception {
        mockMvc.perform(roleRequest(EUserRole.DEVELOPER).with(authentication(authenticationFor(reporter))).with(csrf()))
                .andExpect(status().isCreated());
        mockMvc.perform(roleRequest(EUserRole.DEVELOPER).with(authentication(authenticationFor(reporter))).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUPPORT_REQUEST_ALREADY_PENDING"));
        assertThat(requests.count()).isEqualTo(1);
    }

    @Test
    void concurrentHttpRequestsCreateOnlyOnePendingRoleChange() throws Exception {
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> submitConcurrentRoleChange(start));
            var second = executor.submit(() -> submitConcurrentRoleChange(start));
            start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(requests.count()).isEqualTo(1);
    }

    @Test
    void databaseConstraintProtectsConcurrentWritesWithoutServicePrecheck() throws Exception {
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> saveConcurrentRoleChange(start));
            var second = executor.submit(() -> saveConcurrentRoleChange(start));
            start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(requests.count()).isEqualTo(1);
    }

    @Test
    void resolvedHistoryDoesNotBlockNewPendingRequestsAndDifferentUsersDoNotConflict() throws Exception {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        for (int i = 0; i < 2; i++) {
            requests.save(new SupportRequest(null, reporter.getId(), ESupportRequestType.ROLE_CHANGE,
                    EUserRole.DEVELOPER, "Previous request", ESupportRequestStatus.REJECTED,
                    createdAt, createdAt.plusHours(1)));
        }
        var anotherReporter = saveUser("AnotherReporter", EUserRole.REPORTER);
        mockMvc.perform(roleRequest(EUserRole.DEVELOPER).with(authentication(authenticationFor(reporter))).with(csrf()))
                .andExpect(status().isCreated());
        mockMvc.perform(roleRequest(EUserRole.DEVELOPER).with(authentication(authenticationFor(anotherReporter))).with(csrf()))
                .andExpect(status().isCreated());
        assertThat(requests.count()).isEqualTo(4);
    }

    @Test
    void directServiceCallsEnforceAuthorizationWithoutHttpSecurityRules() {
        var input = new CreateSupportRequestRequest(ESupportRequestType.OTHER, null, "Help");
        assertThatThrownBy(() -> service.create(input)).isInstanceOf(AuthenticationCredentialsNotFoundException.class);

        SecurityContextHolder.getContext().setAuthentication(authenticationFor(admin));
        assertThatThrownBy(() -> service.create(input)).isInstanceOf(AccessDeniedException.class);
        assertThat(requests.count()).isZero();
    }

    @Test
    void existingSessionUsesCurrentRoleWhenAccountBecomesAdministrator() throws Exception {
        var staleAuthentication = authenticationFor(reporter);
        reporter.setRole(EUserRole.ADMIN);
        users.save(reporter);

        mockMvc.perform(generalRequest("Help").with(authentication(staleAuthentication)).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(requests.count()).isZero();
    }

    /** Read the stored request independently of the creation confirmation response. */
    private SupportRequest onlySavedRequest() {
        String id = jdbc.queryForObject("SELECT id FROM support_request", String.class);
        return requests.findById(UUID.fromString(id)).orElseThrow();
    }

    private int submitConcurrentRoleChange(CountDownLatch start) throws Exception {
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("Concurrent request start timed out");
        }
        return mockMvc.perform(roleRequest(EUserRole.DEVELOPER)
                .with(authentication(authenticationFor(reporter))).with(csrf())).andReturn().getResponse().getStatus();
    }

    private boolean saveConcurrentRoleChange(CountDownLatch start) throws Exception {
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("Concurrent write start timed out");
        }
        try {
            requests.save(new SupportRequest(reporter.getId(), ESupportRequestType.ROLE_CHANGE,
                    EUserRole.DEVELOPER, null, LocalDateTime.now()));
            return true;
        } catch (DataIntegrityViolationException expected) {
            return false;
        }
    }

    private MockHttpServletRequestBuilder roleRequest(EUserRole role) {
        return post("/api/support").contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of("type", "ROLE_CHANGE", "requestedRole", role)));
    }

    private MockHttpServletRequestBuilder generalRequest(String description) {
        return post("/api/support").contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of("type", "OTHER", "description", description)));
    }

    private Authentication authenticationFor(UserAccount account) {
        var principal = new UserAccountDetails(account);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private UserAccount saveUser(String name, EUserRole role) {
        return users.save(new UserAccount(name, name.toLowerCase(java.util.Locale.ROOT) + "@example.com", "hash", role));
    }
}
