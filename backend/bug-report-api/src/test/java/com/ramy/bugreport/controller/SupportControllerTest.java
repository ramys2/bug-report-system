package com.ramy.bugreport.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ramy.bugreport.domain.ESupportRequestType;
import com.ramy.bugreport.domain.ESupportRequestStatus;
import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.dto.support.CreateSupportRequestRequest;
import com.ramy.bugreport.dto.support.CreateSupportRequestResponse;
import com.ramy.bugreport.dto.support.SupportRequestResponse;
import com.ramy.bugreport.dto.support.UpdateSupportRequestStatusRequest;
import com.ramy.bugreport.dto.support.UpdateSupportRequestStatusResponse;
import com.ramy.bugreport.exception.ApiExceptionHandler;
import com.ramy.bugreport.service.SupportService;

@ExtendWith(MockitoExtension.class)
class SupportControllerTest {
    @Mock private SupportService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SupportController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void creationUsesSupportRouteAndReturnsOnlyCodeAndMessage() throws Exception {
        var request = new CreateSupportRequestRequest(ESupportRequestType.ROLE_CHANGE, EUserRole.DEVELOPER, null);
        when(service.create(request)).thenReturn(new CreateSupportRequestResponse(
                "SUPPORT_REQUEST_CREATED", "Support request created successfully."));

        mockMvc.perform(post("/api/support").contentType("application/json")
                        .content("{\"type\":\"ROLE_CHANGE\",\"requestedRole\":\"DEVELOPER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUPPORT_REQUEST_CREATED"))
                .andExpect(jsonPath("$.message").value("Support request created successfully."))
                .andExpect(jsonPath("$.length()").value(2));
        verify(service).create(request);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"type\":\"UNKNOWN\"}",
            "{\"type\":\"ROLE_CHANGE\"}",
            "{\"type\":\"ROLE_CHANGE\",\"requestedRole\":\"UNKNOWN\"}",
            "{\"type\":\"OTHER\"}",
            "{\"type\":\"OTHER\",\"description\":\"\"}",
            "{\"type\":\"OTHER\",\"description\":\" \\n\\t\"}",
            "{\"type\":\"OTHER\",\"description\":\"Help\",\"requestedRole\":\"DEVELOPER\"}"
    })
    void invalidRequestDetailsAreRejectedBeforeServiceCall(String body) throws Exception {
        mockMvc.perform(post("/api/support").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void descriptionLengthIsBoundedBeforePersistence() throws Exception {
        mockMvc.perform(post("/api/support").contentType("application/json")
                        .content("{\"type\":\"OTHER\",\"description\":\"%s\"}".formatted("x".repeat(10001))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void listingReturnsRequestDetailsAndRequesterName() throws Exception {
        var id = UUID.randomUUID();
        when(service.getAll()).thenReturn(List.of(new SupportRequestResponse(id, UUID.randomUUID(), "Requester",
                ESupportRequestType.OTHER, null, "Help", ESupportRequestStatus.PENDING, LocalDateTime.now(), null)));

        mockMvc.perform(get("/api/support"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].requesterName").value("Requester"))
                .andExpect(jsonPath("$[0].description").value("Help"));
    }

    @Test
    void resolutionReturnsOnlyCodeAndMessage() throws Exception {
        var id = UUID.randomUUID();
        when(service.updateStatus(id, new UpdateSupportRequestStatusRequest(ESupportRequestStatus.APPROVED)))
                .thenReturn(new UpdateSupportRequestStatusResponse("SUPPORT_REQUEST_RESOLVED", "Support request resolved successfully."));

        mockMvc.perform(patch("/api/support/{id}/status", id).contentType("application/json")
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUPPORT_REQUEST_RESOLVED"))
                .andExpect(jsonPath("$.message").value("Support request resolved successfully."))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"status\":null}", "{\"status\":\"UNKNOWN\"}"})
    void invalidResolutionBodiesAreRejected(String body) throws Exception {
        mockMvc.perform(patch("/api/support/{id}/status", UUID.randomUUID()).contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
