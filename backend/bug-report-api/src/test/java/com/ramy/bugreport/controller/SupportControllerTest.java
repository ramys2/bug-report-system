package com.ramy.bugreport.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.dto.support.CreateSupportRequestRequest;
import com.ramy.bugreport.dto.support.CreateSupportRequestResponse;
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
}
