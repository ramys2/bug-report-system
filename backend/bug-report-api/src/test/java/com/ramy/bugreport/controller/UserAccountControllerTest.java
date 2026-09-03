package com.ramy.bugreport.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.dto.account.CreateUserAccountRequest;
import com.ramy.bugreport.dto.account.CreateUserAccountResponse;
import com.ramy.bugreport.dto.account.DeveloperResponse;
import com.ramy.bugreport.dto.account.UpdateRoleRequest;
import com.ramy.bugreport.dto.account.UserAccountResponse;
import com.ramy.bugreport.exception.ApiExceptionHandler;
import com.ramy.bugreport.service.UserAccountService;

@ExtendWith(MockitoExtension.class)
class UserAccountControllerTest {

    @Mock private UserAccountService userAccountService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserAccountController(userAccountService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void getAllUsesAccountsRoute() throws Exception {
        var accountId = UUID.randomUUID();
        when(userAccountService.getAll()).thenReturn(List.of(new UserAccountResponse(
                accountId, "Ramy", "ramy@example.com", EUserRole.REPORTER)));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(accountId.toString()))
                .andExpect(jsonPath("$[0].username").value("Ramy"))
                .andExpect(jsonPath("$[0].email").value("ramy@example.com"))
                .andExpect(jsonPath("$[0].role").value("REPORTER"));

        verify(userAccountService).getAll();
    }

    @Test
    void getDevelopersUsesDevelopersRoute() throws Exception {
        var developerId = UUID.randomUUID();
        when(userAccountService.getDevelopers())
                .thenReturn(List.of(new DeveloperResponse(developerId, "Ada Lovelace")));

        mockMvc.perform(get("/api/accounts/developers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(developerId.toString()))
                .andExpect(jsonPath("$[0].name").value("Ada Lovelace"));
    }

    @Test
    void createUsesAccountsRouteAndReturnsCreated() throws Exception {
        var request = new CreateUserAccountRequest("Ramy", "ramy@example.com", "Password123!");
        var accountId = UUID.randomUUID();
        when(userAccountService.create(request))
                .thenReturn(new CreateUserAccountResponse(accountId, "Successfully created!"));

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"Ramy","email":"ramy@example.com","password":"Password123!"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(accountId.toString()));

        verify(userAccountService).create(request);
    }

    @Test
    void updateRoleReturnsNoContent() throws Exception {
        var accountId = UUID.randomUUID();

        mockMvc.perform(patch("/api/accounts/{userId}/role", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"DEVELOPER\"}"))
                .andExpect(status().isNoContent());

        verify(userAccountService).updateRole(accountId, new UpdateRoleRequest(EUserRole.DEVELOPER));
    }

    @Test
    void updateRoleRejectsMissingRole() throws Exception {
        mockMvc.perform(patch("/api/accounts/{userId}/role", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request contains invalid values."));
    }
}
