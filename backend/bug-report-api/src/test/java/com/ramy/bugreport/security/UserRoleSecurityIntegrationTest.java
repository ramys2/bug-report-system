package com.ramy.bugreport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.repository.IUserAccountRepository;

@SpringBootTest
@WebAppConfiguration
class UserRoleSecurityIntegrationTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private IUserAccountRepository userAccountRepository;

    private MockMvc mockMvc;
    private UserAccount admin;
    private UserAccount reporter;
    private UserAccount developer;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        userAccountRepository.deleteAll();
        admin = save("admin@example.com", EUserRole.ADMIN);
        reporter = save("reporter@example.com", EUserRole.REPORTER);
        developer = save("developer@example.com", EUserRole.DEVELOPER);
    }

    @Test
    void unauthenticatedRoleUpdateReturnsJson401() throws Exception {
        mockMvc.perform(roleUpdate(reporter.getId()).with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void reporterAndDeveloperCannotUpdateRoles() throws Exception {
        mockMvc.perform(roleUpdate(reporter.getId()).with(authentication(authenticationFor(reporter))).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(roleUpdate(reporter.getId()).with(authentication(authenticationFor(developer))).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotChangeOwnRole() throws Exception {
        mockMvc.perform(roleUpdate(admin.getId()).with(authentication(authenticationFor(admin))).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void csrfIsRequiredForRoleUpdate() throws Exception {
        mockMvc.perform(roleUpdate(reporter.getId()).with(authentication(authenticationFor(admin))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUpdateAnotherUsersRole() throws Exception {
        mockMvc.perform(roleUpdate(reporter.getId()).with(authentication(authenticationFor(admin))).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(userAccountRepository.findById(reporter.getId()).orElseThrow().getRole())
                .isEqualTo(EUserRole.DEVELOPER);
    }

    @Test
    void activeSessionUsesCurrentRoleOnTheNextRequest() throws Exception {
        var secondAdmin = save("second-admin@example.com", EUserRole.ADMIN);
        Authentication staleAdminSession = authenticationFor(secondAdmin);

        mockMvc.perform(roleUpdate(secondAdmin.getId())
                        .with(authentication(authenticationFor(admin)))
                        .with(csrf())
                        .content("{\"role\":\"REPORTER\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(roleUpdate(reporter.getId()).with(authentication(staleAdminSession)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder roleUpdate(UUID userId) {
        return patch("/api/accounts/{userId}/role", userId)
                .contentType("application/json")
                .content("{\"role\":\"DEVELOPER\"}");
    }

    private Authentication authenticationFor(UserAccount account) {
        var details = new UserAccountDetails(account);
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    private UserAccount save(String emailAddress, EUserRole role) {
        return userAccountRepository.saveAndFlush(new UserAccount(emailAddress, emailAddress, "hash", role));
    }
}
