package com.ramy.bugreport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ramy.bugreport.domain.ESupportRequestStatus;
import com.ramy.bugreport.domain.ESupportRequestType;
import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.SupportRequest;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.dto.support.UpdateSupportRequestStatusRequest;
import com.ramy.bugreport.repository.IBugReportRepository;
import com.ramy.bugreport.repository.ICommentRepository;
import com.ramy.bugreport.repository.IComponentRepository;
import com.ramy.bugreport.repository.IResolutionRepository;
import com.ramy.bugreport.repository.ISoftwareProjectRepository;
import com.ramy.bugreport.repository.ISupportRequestRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;
import com.ramy.bugreport.service.SupportService;

import jakarta.persistence.EntityManager;

/** Exercises real security, role management, request/account row locks and transaction rollback in MariaDB. */
@SpringBootTest
@WebAppConfiguration
@ActiveProfiles("test")
class SupportAdministrationIT {
    @Autowired private WebApplicationContext context;
    @MockitoSpyBean private ISupportRequestRepository requests;
    @Autowired private IUserAccountRepository users;
    @Autowired private IBugReportRepository reports;
    @Autowired private ICommentRepository comments;
    @Autowired private IComponentRepository components;
    @Autowired private ISoftwareProjectRepository projects;
    @Autowired private IResolutionRepository resolutions;
    @Autowired private SupportService service;
    @Autowired private EntityManager entityManager;

    private MockMvc mockMvc;
    private UserAccount admin;
    private UserAccount reporter;
    private UserAccount developer;

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
        admin = saveUser("Admin", EUserRole.ADMIN);
        reporter = saveUser("Reporter", EUserRole.REPORTER);
        developer = saveUser("Developer", EUserRole.DEVELOPER);
    }

    @AfterEach
    void cleanUp() {
        requests.deleteAll();
        SecurityContextHolder.clearContext();
    }

    @Test
    void administratorListsAllRequestsNewestFirstWithRequesterDetails() throws Exception {
        var earlier = requests.save(new SupportRequest(reporter.getId(), ESupportRequestType.ROLE_CHANGE,
                EUserRole.DEVELOPER, null, LocalDateTime.now().minusDays(1)));
        var later = requests.save(new SupportRequest(developer.getId(), ESupportRequestType.OTHER,
                null, "Please help", LocalDateTime.now()));

        mockMvc.perform(get("/api/support").with(authentication(authenticationFor(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(later.getId().toString()))
                .andExpect(jsonPath("$[0].requesterName").value("Developer"))
                .andExpect(jsonPath("$[0].description").value("Please help"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].id").value(earlier.getId().toString()))
                .andExpect(jsonPath("$[1].requesterId").value(reporter.getId().toString()))
                .andExpect(jsonPath("$[1].requestedRole").value("DEVELOPER"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @ParameterizedTest
    @EnumSource(value = EUserRole.class, names = {"REPORTER", "DEVELOPER"})
    void nonAdministratorsCannotListOrResolveRequests(EUserRole role) throws Exception {
        var account = role == EUserRole.REPORTER ? reporter : developer;
        var request = roleChange();
        mockMvc.perform(get("/api/support").with(authentication(authenticationFor(account))))
                .andExpect(status().isForbidden());
        mockMvc.perform(resolution(request.getId(), ESupportRequestStatus.APPROVED)
                        .with(authentication(authenticationFor(account))).with(csrf()))
                .andExpect(status().isForbidden());
        assertPending(request);
    }

    @Test
    void anonymousUsersCannotListOrResolveRequests() throws Exception {
        var request = roleChange();
        mockMvc.perform(get("/api/support")).andExpect(status().isUnauthorized());
        mockMvc.perform(resolution(request.getId(), ESupportRequestStatus.REJECTED).with(csrf()))
                .andExpect(status().isUnauthorized());
        assertPending(request);
    }

    @Test
    void resolutionRequiresCsrfToken() throws Exception {
        var request = roleChange();
        mockMvc.perform(resolution(request.getId(), ESupportRequestStatus.APPROVED)
                        .with(authentication(authenticationFor(admin))))
                .andExpect(status().isForbidden());
        assertPending(request);
    }

    @Test
    void administratorApprovesValidRoleChangeAndCommitsBothUpdates() throws Exception {
        var request = roleChange();
        var storedCreatedAt = requests.findById(request.getId()).orElseThrow().getCreatedAt();
        var before = LocalDateTime.now().minusSeconds(1);

        mockMvc.perform(resolution(request.getId(), ESupportRequestStatus.APPROVED)
                        .with(authentication(authenticationFor(admin))).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUPPORT_REQUEST_RESOLVED"))
                .andExpect(jsonPath("$.message").value("Support request resolved successfully."))
                .andExpect(jsonPath("$.length()").value(2));

        var resolved = requests.findById(request.getId()).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo(ESupportRequestStatus.APPROVED);
        assertThat(resolved.getResolvedAt()).isBetween(before, LocalDateTime.now());
        assertThat(resolved.getCreatedAt()).isEqualTo(storedCreatedAt);
        assertThat(users.findById(reporter.getId()).orElseThrow().getRole()).isEqualTo(EUserRole.DEVELOPER);
    }

    @ParameterizedTest
    @EnumSource(value = ESupportRequestStatus.class, names = {"APPROVED", "REJECTED"})
    void generalRequestsResolveWithoutChangingRoles(ESupportRequestStatus status) throws Exception {
        var request = requests.save(new SupportRequest(reporter.getId(), ESupportRequestType.OTHER,
                null, "Help", LocalDateTime.now()));
        mockMvc.perform(resolution(request.getId(), status).with(authentication(authenticationFor(admin))).with(csrf()))
                .andExpect(status().isOk());

        var resolved = requests.findById(request.getId()).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo(status);
        assertThat(resolved.getResolvedAt()).isNotNull();
        assertThat(users.findById(reporter.getId()).orElseThrow().getRole()).isEqualTo(EUserRole.REPORTER);
    }

    @Test
    void rejectingRoleChangeDoesNotChangeRoleAndAllowsNewRequest() throws Exception {
        var request = roleChange();
        mockMvc.perform(resolution(request.getId(), ESupportRequestStatus.REJECTED)
                        .with(authentication(authenticationFor(admin))).with(csrf()))
                .andExpect(status().isOk());
        assertThat(users.findById(reporter.getId()).orElseThrow().getRole()).isEqualTo(EUserRole.REPORTER);
        assertThat(requests.findById(request.getId()).orElseThrow().getStatus()).isEqualTo(ESupportRequestStatus.REJECTED);
        assertThat(roleChange().getId()).isNotEqualTo(request.getId());
    }

    @ParameterizedTest
    @EnumSource(value = ESupportRequestStatus.class, names = {"APPROVED", "REJECTED"})
    void resolvedRequestsCannotBeProcessedAgain(ESupportRequestStatus firstStatus) throws Exception {
        var request = roleChange();
        mockMvc.perform(resolution(request.getId(), firstStatus).with(authentication(authenticationFor(admin))).with(csrf()))
                .andExpect(status().isOk());
        var resolvedAt = requests.findById(request.getId()).orElseThrow().getResolvedAt();

        mockMvc.perform(resolution(request.getId(), ESupportRequestStatus.REJECTED)
                        .with(authentication(authenticationFor(admin))).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUPPORT_REQUEST_ALREADY_RESOLVED"));
        var saved = requests.findById(request.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(firstStatus);
        assertThat(saved.getResolvedAt()).isEqualTo(resolvedAt);
    }

    @ParameterizedTest
    @EnumSource(value = EUserRole.class, names = {"DEVELOPER", "ADMIN"})
    void outdatedRoleChangesCannotBeApproved(EUserRole currentRole) throws Exception {
        var request = roleChange();
        reporter.setRole(currentRole);
        users.save(reporter);

        mockMvc.perform(resolution(request.getId(), ESupportRequestStatus.APPROVED)
                        .with(authentication(authenticationFor(admin))).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUPPORT_ROLE_CHANGE_OUTDATED"));
        assertPending(request);
        assertThat(users.findById(reporter.getId()).orElseThrow().getRole()).isEqualTo(currentRole);
    }

    @Test
    void pendingIsNotAValidResolution() throws Exception {
        var request = roleChange();
        mockMvc.perform(resolution(request.getId(), ESupportRequestStatus.PENDING)
                        .with(authentication(authenticationFor(admin))).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUPPORT_INVALID_RESOLUTION_STATUS"));
        assertPending(request);
    }

    @Test
    void unknownRequestReturns404() throws Exception {
        mockMvc.perform(resolution(UUID.randomUUID(), ESupportRequestStatus.APPROVED)
                        .with(authentication(authenticationFor(admin))).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SUPPORT_REQUEST_NOT_FOUND"));
    }

    @Test
    void administratorCannotApproveOwnHistoricalRequest() throws Exception {
        var request = requests.save(new SupportRequest(admin.getId(), ESupportRequestType.OTHER,
                null, "Historical request", LocalDateTime.now().minusDays(1)));
        mockMvc.perform(resolution(request.getId(), ESupportRequestStatus.APPROVED)
                        .with(authentication(authenticationFor(admin))).with(csrf()))
                .andExpect(status().isForbidden());
        assertPending(request);
    }

    @Test
    void concurrentApprovalAndRejectionResolveExactlyOnce() throws Exception {
        var request = roleChange();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var approval = executor.submit(() -> concurrentResolution(start, request.getId(), ESupportRequestStatus.APPROVED));
            var rejection = executor.submit(() -> concurrentResolution(start, request.getId(), ESupportRequestStatus.REJECTED));
            start.countDown();
            assertThat(List.of(approval.get(15, TimeUnit.SECONDS), rejection.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        }
        var resolved = requests.findById(request.getId()).orElseThrow();
        assertThat(resolved.getResolvedAt()).isNotNull();
        var expectedRole = resolved.getStatus() == ESupportRequestStatus.APPROVED ? EUserRole.DEVELOPER : EUserRole.REPORTER;
        assertThat(users.findById(reporter.getId()).orElseThrow().getRole()).isEqualTo(expectedRole);
    }

    @Test
    void failureAfterFlushingBothUpdatesRollsBackRoleAndResolution() throws Exception {
        var request = roleChange();
        // Stub the underlying spy, keeping Spring's transaction proxy active for real calls.
        ISupportRequestRepository repositorySpy = AopTestUtils.getUltimateTargetObject(requests);
        doAnswer(invocation -> {
            invocation.callRealMethod();
            entityManager.flush();
            throw new DataIntegrityViolationException("Forced failure after database writes");
        }).when(repositorySpy).save(any(SupportRequest.class));

        mockMvc.perform(resolution(request.getId(), ESupportRequestStatus.APPROVED)
                        .with(authentication(authenticationFor(admin))).with(csrf()))
                .andExpect(status().isConflict());

        assertPending(request);
        assertThat(users.findById(reporter.getId()).orElseThrow().getRole()).isEqualTo(EUserRole.REPORTER);
    }

    @Test
    void serviceAuthorizationProtectsDirectCalls() {
        var request = roleChange();
        var update = new UpdateSupportRequestStatusRequest(ESupportRequestStatus.APPROVED);
        assertThatThrownBy(() -> service.getAll()).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(() -> service.updateStatus(request.getId(), update))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        SecurityContextHolder.getContext().setAuthentication(authenticationFor(reporter));
        assertThatThrownBy(() -> service.getAll()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.updateStatus(request.getId(), update)).isInstanceOf(AccessDeniedException.class);
        assertPending(request);
    }

    private int concurrentResolution(CountDownLatch start, UUID id, ESupportRequestStatus status) throws Exception {
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("Concurrent resolution start timed out");
        }
        return mockMvc.perform(resolution(id, status).with(authentication(authenticationFor(admin))).with(csrf()))
                .andReturn().getResponse().getStatus();
    }

    private void assertPending(SupportRequest request) {
        var saved = requests.findById(request.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ESupportRequestStatus.PENDING);
        assertThat(saved.getResolvedAt()).isNull();
    }

    private SupportRequest roleChange() {
        return requests.save(new SupportRequest(reporter.getId(), ESupportRequestType.ROLE_CHANGE,
                EUserRole.DEVELOPER, null, LocalDateTime.now().minusSeconds(1)));
    }

    private MockHttpServletRequestBuilder resolution(UUID id, ESupportRequestStatus status) {
        return patch("/api/support/{id}/status", id).contentType("application/json")
                .content("{\"status\":\"%s\"}".formatted(status));
    }

    private Authentication authenticationFor(UserAccount account) {
        var principal = new UserAccountDetails(account);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private UserAccount saveUser(String name, EUserRole role) {
        return users.save(new UserAccount(name, name.toLowerCase(java.util.Locale.ROOT) + "@example.com", "hash", role));
    }
}
