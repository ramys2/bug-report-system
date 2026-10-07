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
import com.ramy.bugreport.dto.PageResponse;
import com.ramy.bugreport.dto.account.CreateUserAccountRequest;
import com.ramy.bugreport.dto.account.CreateUserAccountResponse;
import com.ramy.bugreport.dto.account.DeveloperResponse;
import com.ramy.bugreport.dto.account.UpdateRoleRequest;
import com.ramy.bugreport.dto.account.UpdateUserAccountResponse;
import com.ramy.bugreport.dto.account.UserAccountResponse;
import com.ramy.bugreport.dto.account.UserAccountBriefResponse;
import com.ramy.bugreport.exception.ApiExceptionHandler;
import com.ramy.bugreport.repository.UserAccountFilter;
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
    void getAllUsesDefaultPagingAndNoFilters() throws Exception {
        var accountId = UUID.randomUUID();
        var noFilter = new UserAccountFilter(null, null, null, null);
        when(userAccountService.getAll(noFilter, 0, 10)).thenReturn(PageResponse.of(
                List.of(new UserAccountResponse(accountId, "Ramy", "ramy@example.com", EUserRole.REPORTER)),
                0, 10, 1));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(accountId.toString()))
                .andExpect(jsonPath("$.items[0].name").value("Ramy"))
                .andExpect(jsonPath("$.items[0].email").value("ramy@example.com"))
                .andExpect(jsonPath("$.items[0].role").value("REPORTER"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(userAccountService).getAll(noFilter, 0, 10);
    }

    @Test
    void getAllPassesFiltersAndPagingToService() throws Exception {
        var filter = new UserAccountFilter("3f2a", "ali", "example.com", EUserRole.DEVELOPER);
        when(userAccountService.getAll(filter, 2, 5)).thenReturn(PageResponse.of(List.of(), 2, 5, 11));

        mockMvc.perform(get("/api/accounts")
                        .param("id", "3f2a")
                        .param("name", "ali")
                        .param("email", "example.com")
                        .param("role", "DEVELOPER")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalPages").value(3));

        verify(userAccountService).getAll(filter, 2, 5);
    }

    @Test
    void getAllTreatsEmptyFilterValuesAsNoFilter() throws Exception {
        var noFilter = new UserAccountFilter("", "", "", null);
        when(userAccountService.getAll(noFilter, 0, 10)).thenReturn(PageResponse.of(List.of(), 0, 10, 0));

        mockMvc.perform(get("/api/accounts")
                        .param("id", "")
                        .param("name", "")
                        .param("email", "")
                        .param("role", "")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPages").value(0));

        verify(userAccountService).getAll(noFilter, 0, 10);
    }

    @Test
    void getAllRejectsUnknownRole() throws Exception {
        mockMvc.perform(get("/api/accounts").param("role", "BOSS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request contains invalid values."));
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
    void searchUsersUsesSearchQueryAndReturnsBriefResponses() throws Exception {
        var accountId = UUID.randomUUID();
        when(userAccountService.searchUsers("ram"))
                .thenReturn(List.of(new UserAccountBriefResponse(accountId, "Ramy")));

        mockMvc.perform(get("/api/accounts/users").param("search", "ram"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(accountId.toString()))
                .andExpect(jsonPath("$[0].name").value("Ramy"));

        verify(userAccountService).searchUsers("ram");
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
                                {"name":"Ramy","email":"ramy@example.com","password":"Password123!"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(accountId.toString()));

        verify(userAccountService).create(request);
    }

    @Test
    void updateRoleReturnsIdAndMessage() throws Exception {
        var accountId = UUID.randomUUID();
        when(userAccountService.updateRole(accountId, new UpdateRoleRequest(EUserRole.DEVELOPER)))
                .thenReturn(new UpdateUserAccountResponse(accountId, "User role updated successfully!"));

        mockMvc.perform(patch("/api/accounts/{userId}/role", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"DEVELOPER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId.toString()))
                .andExpect(jsonPath("$.message").value("User role updated successfully!"));

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
