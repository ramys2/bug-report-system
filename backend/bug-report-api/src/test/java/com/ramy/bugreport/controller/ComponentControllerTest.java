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

import com.ramy.bugreport.dto.component.CreateComponentRequest;
import com.ramy.bugreport.dto.component.CreateComponentResponse;
import com.ramy.bugreport.dto.component.ComponentResponse;
import com.ramy.bugreport.dto.component.UpdateComponentDescriptionRequest;
import com.ramy.bugreport.dto.component.UpdateComponentNameRequest;
import com.ramy.bugreport.dto.component.UpdateComponentResponsibleUserRequest;
import com.ramy.bugreport.dto.component.UpdateComponentResponse;
import com.ramy.bugreport.service.ComponentService;

@ExtendWith(MockitoExtension.class)
class ComponentControllerTest {

    @Mock
    private ComponentService componentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ComponentController(componentService))
                .build();
    }

    @Test
    void getAllUsesComponentsRoute() throws Exception {
        var componentId = UUID.randomUUID();
        when(componentService.getAll()).thenReturn(List.of(new ComponentResponse(
                componentId, "API", "Handles public endpoints", "Joe Responsible")));

        mockMvc.perform(get("/api/components"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(componentId.toString()))
                .andExpect(jsonPath("$[0].name").value("API"))
                .andExpect(jsonPath("$[0].description")
                        .value("Handles public endpoints"))
                .andExpect(jsonPath("$[0].responsibleUserName")
                        .value("Joe Responsible"));

        verify(componentService).getAll();
    }

    @Test
    void createAcceptsMissingOptionalFieldsAndReturnsCreated() throws Exception {
        var request = new CreateComponentRequest("API", null, null);
        var componentId = UUID.randomUUID();
        when(componentService.create(request))
                .thenReturn(new CreateComponentResponse(componentId, "Successfully created!"));

        mockMvc.perform(post("/api/components")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "API"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(componentId.toString()))
                .andExpect(jsonPath("$.message").value("Successfully created!"));

        verify(componentService).create(request);
    }

    @Test
    void dedicatedUpdateRoutesUseComponentIdAndReturnOk() throws Exception {
        var componentId = UUID.randomUUID();
        var responsibleUserId = UUID.randomUUID();
        var response = new UpdateComponentResponse(componentId, "Component updated successfully!");
        var nameRequest = new UpdateComponentNameRequest("Frontend");
        var descriptionRequest = new UpdateComponentDescriptionRequest("Handles the UI");
        var responsibleUserRequest = new UpdateComponentResponsibleUserRequest(responsibleUserId);
        when(componentService.updateName(componentId, nameRequest)).thenReturn(response);
        when(componentService.updateDescription(componentId, descriptionRequest)).thenReturn(response);
        when(componentService.updateResponsibleUserId(componentId, responsibleUserRequest)).thenReturn(response);

        mockMvc.perform(patch("/api/components/{componentId}/name", componentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Frontend\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(componentId.toString()))
                .andExpect(jsonPath("$.message").value("Component updated successfully!"));

        mockMvc.perform(patch("/api/components/{componentId}/description", componentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Handles the UI\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/components/{componentId}/responsibleUserId", componentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsibleUserId\":\"%s\"}".formatted(responsibleUserId)))
                .andExpect(status().isOk());

        verify(componentService).updateName(componentId, nameRequest);
        verify(componentService).updateDescription(componentId, descriptionRequest);
        verify(componentService).updateResponsibleUserId(componentId, responsibleUserRequest);
    }
}
