package com.ramy.bugreport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import com.ramy.bugreport.dto.component.CreateComponentRequest;
import com.ramy.bugreport.dto.component.UpdateComponentDescriptionRequest;
import com.ramy.bugreport.dto.component.UpdateComponentNameRequest;
import com.ramy.bugreport.dto.component.UpdateComponentResponsibleUserRequest;
import com.ramy.bugreport.dto.project.CreateSoftwareProjectRequest;
import com.ramy.bugreport.dto.project.UpdateSoftwareProjectDescriptionRequest;
import com.ramy.bugreport.dto.project.UpdateSoftwareProjectNameRequest;
import com.ramy.bugreport.repository.UserAccountFilter;
import com.ramy.bugreport.service.ComponentService;
import com.ramy.bugreport.service.SoftwareProjectService;
import com.ramy.bugreport.service.UserAccountService;

/**
 * Checks the {@code @PreAuthorize} rules on the services by calling them directly, without HTTP,
 * so the URL rules in {@code SecurityConfig} cannot be what is being tested.
 *
 * <p>For a permitted role the calls use ids that do not exist. They are expected to fail later with
 * "not found"; the tests only check that the failure is not an {@link AccessDeniedException}.
 * The ADMIN test creates a real project; the other integration tests clear the tables in their own setup.
 */
@SpringBootTest
@ActiveProfiles("test")
class ServiceAuthorizationIT {

    @Autowired private UserAccountService userAccountService;
    @Autowired private SoftwareProjectService projectService;
    @Autowired private ComponentService componentService;

    @Test
    void callsWithoutAnAuthenticatedUserAreRejected() {
        for (ThrowingCallable call : allCalls()) {
            assertThat(catchThrowable(call)).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        }
    }

    @Test
    @WithMockUser(roles = "REPORTER")
    void reporterIsDeniedEveryRestrictedMethod() {
        for (ThrowingCallable call : allCalls()) {
            assertDenied(call);
        }
    }

    @Test
    @WithMockUser(roles = "DEVELOPER")
    void developerIsDeniedAdminOnlyMethodsButNotTheOthers() {
        for (ThrowingCallable call : adminOnlyCalls()) {
            assertDenied(call);
        }
        for (ThrowingCallable call : adminOrDeveloperCalls()) {
            assertNotDenied(call);
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminIsDeniedNoRestrictedMethod() {
        for (ThrowingCallable call : allCalls()) {
            assertNotDenied(call);
        }
    }

    private void assertDenied(ThrowingCallable call) {
        assertThat(catchThrowable(call)).isInstanceOf(AccessDeniedException.class);
    }

    /** The call may succeed or fail for another reason (e.g. unknown id), as long as it is not denied. */
    private void assertNotDenied(ThrowingCallable call) {
        assertThat(catchThrowable(call) instanceof AccessDeniedException).isFalse();
    }

    private List<ThrowingCallable> allCalls() {
        return Stream.concat(adminOnlyCalls().stream(), adminOrDeveloperCalls().stream()).toList();
    }

    /** Methods that need the ADMIN role. */
    private List<ThrowingCallable> adminOnlyCalls() {
        return List.of(
                () -> userAccountService.getAll(new UserAccountFilter(null, null, null, null), 0, 10),
                () -> projectService.create(new CreateSoftwareProjectRequest("Project", null)));
    }

    /** Methods that need the ADMIN or DEVELOPER role. */
    private List<ThrowingCallable> adminOrDeveloperCalls() {
        UUID unknownId = UUID.randomUUID();
        return List.of(
                () -> userAccountService.searchUsers("name"),
                () -> projectService.updateName(unknownId, new UpdateSoftwareProjectNameRequest("Name")),
                () -> projectService.updateDescription(unknownId, new UpdateSoftwareProjectDescriptionRequest("Text")),
                () -> componentService.create(new CreateComponentRequest("Name", unknownId, null, null)),
                () -> componentService.updateName(unknownId, new UpdateComponentNameRequest("Name")),
                () -> componentService.updateDescription(unknownId, new UpdateComponentDescriptionRequest("Text")),
                () -> componentService.updateResponsibleUserId(unknownId, new UpdateComponentResponsibleUserRequest(unknownId)));
    }
}
