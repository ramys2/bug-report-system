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

import com.ramy.bugreport.dto.project.CreateSoftwareProjectRequest;
import com.ramy.bugreport.dto.project.CreateSoftwareProjectResponse;
import com.ramy.bugreport.dto.project.SoftwareProjectResponse;
import com.ramy.bugreport.dto.project.UpdateSoftwareProjectDescriptionRequest;
import com.ramy.bugreport.dto.project.UpdateSoftwareProjectNameRequest;
import com.ramy.bugreport.dto.project.UpdateSoftwareProjectResponse;
import com.ramy.bugreport.service.SoftwareProjectService;

@ExtendWith(MockitoExtension.class)
class SoftwareProjectControllerTest {

    @Mock
    private SoftwareProjectService softwareProjectService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SoftwareProjectController(softwareProjectService))
                .build();
    }

    @Test
    void getAllUsesProjectsRoute() throws Exception {
        var projectId = UUID.randomUUID();
        when(softwareProjectService.getAll()).thenReturn(List.of(new SoftwareProjectResponse(
                projectId, "Bug Report System", "Tracks bugs")));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(projectId.toString()))
                .andExpect(jsonPath("$[0].name").value("Bug Report System"))
                .andExpect(jsonPath("$[0].description").value("Tracks bugs"));

        verify(softwareProjectService).getAll();
    }

    @Test
    void createAcceptsMissingOptionalDescriptionAndReturnsCreated() throws Exception {
        var request = new CreateSoftwareProjectRequest("Bug Report System", null);
        var projectId = UUID.randomUUID();
        when(softwareProjectService.create(request))
                .thenReturn(new CreateSoftwareProjectResponse(projectId, "Successfully created!"));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Bug Report System"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.message").value("Successfully created!"));

        verify(softwareProjectService).create(request);
    }

    @Test
    void dedicatedUpdateRoutesUseProjectIdAndReturnOk() throws Exception {
        var projectId = UUID.randomUUID();
        var response = new UpdateSoftwareProjectResponse(projectId, "Project updated successfully!");
        var nameRequest = new UpdateSoftwareProjectNameRequest("Issue Tracker");
        var descriptionRequest = new UpdateSoftwareProjectDescriptionRequest("Tracks reported issues");
        when(softwareProjectService.updateName(projectId, nameRequest)).thenReturn(response);
        when(softwareProjectService.updateDescription(projectId, descriptionRequest)).thenReturn(response);

        mockMvc.perform(patch("/api/projects/{projectId}/name", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Issue Tracker\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.message").value("Project updated successfully!"));

        mockMvc.perform(patch("/api/projects/{projectId}/description", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Tracks reported issues\"}"))
                .andExpect(status().isOk());

        verify(softwareProjectService).updateName(projectId, nameRequest);
        verify(softwareProjectService).updateDescription(projectId, descriptionRequest);
    }
}
