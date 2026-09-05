package com.ramy.bugreport.controller;

import static org.hamcrest.Matchers.nullValue;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ramy.bugreport.domain.EBugSeverity;
import com.ramy.bugreport.domain.EBugStatus;
import com.ramy.bugreport.dto.report.BugReportBriefResponse;
import com.ramy.bugreport.dto.report.BugReportResponse;
import com.ramy.bugreport.dto.report.CloseBugReportRequest;
import com.ramy.bugreport.dto.report.CloseBugReportResponse;
import com.ramy.bugreport.dto.report.CreateBugReportRequest;
import com.ramy.bugreport.dto.report.CreateBugReportResponse;
import com.ramy.bugreport.dto.report.UpdateActualBehaviorRequest;
import com.ramy.bugreport.dto.report.UpdateAssigneeRequest;
import com.ramy.bugreport.dto.report.UpdateBugReportResponse;
import com.ramy.bugreport.dto.report.UpdateComponentRequest;
import com.ramy.bugreport.dto.report.UpdateDescriptionRequest;
import com.ramy.bugreport.dto.report.UpdateExpectedBehaviorRequest;
import com.ramy.bugreport.dto.report.UpdateProjectRequest;
import com.ramy.bugreport.dto.report.UpdateSeverityRequest;
import com.ramy.bugreport.dto.report.UpdateStatusRequest;
import com.ramy.bugreport.dto.report.UpdateStepsToReproduceRequest;
import com.ramy.bugreport.security.UserAccountDetails;
import com.ramy.bugreport.service.BugReportService;

@ExtendWith(MockitoExtension.class)
class BugReportControllerTest {

    @Mock
    private BugReportService reportService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BugReportController(reportService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllUsesCollectionRoute() throws Exception {
        var reportId = UUID.randomUUID();
        when(reportService.getAll()).thenReturn(List.of(briefResponse(reportId)));

        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reportId").value(reportId.toString()))
                .andExpect(jsonPath("$[0].author").value("Leo Tester"))
                .andExpect(jsonPath("$[0].assignee").value(nullValue()))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].createdAt").value("13-07-2026 12:05"));

        verify(reportService).getAll();
    }

    @Test
    void getReportUsesReportIdPath() throws Exception {
        var reportId = UUID.randomUUID();
        when(reportService.getReport(reportId)).thenReturn(reportResponse(reportId));

        mockMvc.perform(get("/api/reports/{reportId}", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reportId.toString()))
                .andExpect(jsonPath("$.reporterName").value("Joe Reporter"))
                .andExpect(jsonPath("$.assigneeName").value(nullValue()))
                .andExpect(jsonPath("$.projectName").value("Bug Report"))
                .andExpect(jsonPath("$.componentName").value("Backend API"))
                .andExpect(jsonPath("$.reporterId").doesNotExist())
                .andExpect(jsonPath("$.assigneeId").doesNotExist())
                .andExpect(jsonPath("$.projectId").doesNotExist())
                .andExpect(jsonPath("$.componentId").doesNotExist());

        verify(reportService).getReport(reportId);
    }

    @Test
    void getReportSerializesAssignedAssigneeName() throws Exception {
        var reportId = UUID.randomUUID();
        when(reportService.getReport(reportId)).thenReturn(reportResponse(reportId, "Joe Developer"));

        mockMvc.perform(get("/api/reports/{reportId}", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeName").value("Joe Developer"));

        verify(reportService).getReport(reportId);
    }

    @Test
    void getReportedUsesAuthenticatedAccountId() throws Exception {
        var reporterId = UUID.randomUUID();
        var reportId = UUID.randomUUID();
        when(reportService.getReportsByReporter(reporterId))
                .thenReturn(List.of(briefResponse(reportId)));

        authenticate(reporterId);

        mockMvc.perform(get("/api/reports/reported"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reportId").value(reportId.toString()));

        verify(reportService).getReportsByReporter(reporterId);
    }

    @Test
    void getAssignedUsesAuthenticatedAccountId() throws Exception {
        var assigneeId = UUID.randomUUID();
        var reportId = UUID.randomUUID();
        when(reportService.getReportsByAssignee(assigneeId))
                .thenReturn(List.of(briefResponse(reportId)));

        authenticate(assigneeId);

        mockMvc.perform(get("/api/reports/assigned"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reportId").value(reportId.toString()));

        verify(reportService).getReportsByAssignee(assigneeId);
    }

    @Test
    void createUsesCollectionRouteAndReturnsCreated() throws Exception {
        var request = createRequest();
        var reporterId = UUID.randomUUID();
        var reportId = UUID.randomUUID();
        when(reportService.create(reporterId, request))
                .thenReturn(new CreateBugReportResponse(reportId, "Successfully created!"));

        authenticate(reporterId);

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(reportId.toString()))
                .andExpect(jsonPath("$.message").value("Successfully created!"));

        verify(reportService).create(reporterId, request);
    }

    @Test
    void closeUsesResolutionSubresourceRouteAndReturnsCreated() throws Exception {
        var reportId = UUID.randomUUID();
        var resolutionId = UUID.randomUUID();
        var request = new CloseBugReportRequest(
                "Fixed", "1.1.0", "https://example.com/commit/1");
        when(reportService.close(reportId, request))
                .thenReturn(new CloseBugReportResponse(
                        resolutionId, "Task has been closed successfully!"));

        mockMvc.perform(post("/api/reports/{reportId}/resolution", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Fixed",
                                  "fixedVersion": "1.1.0",
                                  "commitUrl": "https://example.com/commit/1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").value(resolutionId.toString()))
                .andExpect(jsonPath("$.message").value("Task has been closed successfully!"));

        verify(reportService).close(reportId, request);
    }

    @Test
    void closeRejectsBlankResolutionDescription() throws Exception {
        mockMvc.perform(post("/api/reports/{reportId}/resolution", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "   ",
                                  "fixedVersion": "1.1.0",
                                  "commitUrl": "https://example.com/commit/1"
                                }
                                """))
                .andExpect(status().isBadRequest());

        org.mockito.Mockito.verifyNoInteractions(reportService);
    }

    @Test
    void dedicatedUpdateRoutesUseReportIdAndReturnOk() throws Exception {
        var reportId = UUID.randomUUID();
        var assigneeId = UUID.randomUUID();
        var projectId = UUID.randomUUID();
        var componentId = UUID.randomUUID();
        var response = new UpdateBugReportResponse(reportId, "Bug report updated successfully!");
        var assigneeRequest = new UpdateAssigneeRequest(assigneeId);
        var severityRequest = new UpdateSeverityRequest(EBugSeverity.CRITICAL);
        var statusRequest = new UpdateStatusRequest(EBugStatus.IN_PROGRESS);
        var projectRequest = new UpdateProjectRequest(projectId);
        var componentRequest = new UpdateComponentRequest(componentId);
        var descriptionRequest = new UpdateDescriptionRequest("Updated description");
        var stepsRequest = new UpdateStepsToReproduceRequest("Updated steps");
        var expectedRequest = new UpdateExpectedBehaviorRequest("Updated expected behavior");
        var actualRequest = new UpdateActualBehaviorRequest("Updated actual behavior");
        when(reportService.updateAssignee(reportId, assigneeRequest)).thenReturn(response);
        when(reportService.updateSeverity(reportId, severityRequest)).thenReturn(response);
        when(reportService.updateStatus(reportId, statusRequest)).thenReturn(response);
        when(reportService.updateProject(reportId, projectRequest)).thenReturn(response);
        when(reportService.updateComponent(reportId, componentRequest)).thenReturn(response);
        when(reportService.updateDescription(reportId, descriptionRequest)).thenReturn(response);
        when(reportService.updateStepsToReproduce(reportId, stepsRequest)).thenReturn(response);
        when(reportService.updateExpectedBehavior(reportId, expectedRequest)).thenReturn(response);
        when(reportService.updateActualBehavior(reportId, actualRequest)).thenReturn(response);

        mockMvc.perform(patch("/api/reports/{reportId}/assignee", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":\"%s\"}".formatted(assigneeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reportId.toString()))
                .andExpect(jsonPath("$.message").value("Bug report updated successfully!"));

        mockMvc.perform(patch("/api/reports/{reportId}/severity", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"severity\":\"CRITICAL\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/reports/{reportId}/status", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/reports/{reportId}/project", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":\"%s\"}".formatted(projectId)))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/reports/{reportId}/component", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"componentId\":\"%s\"}".formatted(componentId)))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/reports/{reportId}/description", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Updated description\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/reports/{reportId}/steps-to-reproduce", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stepsToReproduce\":\"Updated steps\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/reports/{reportId}/expected-behavior", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedBehavior\":\"Updated expected behavior\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/reports/{reportId}/actual-behavior", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualBehavior\":\"Updated actual behavior\"}"))
                .andExpect(status().isOk());

        verify(reportService).updateAssignee(reportId, assigneeRequest);
        verify(reportService).updateSeverity(reportId, severityRequest);
        verify(reportService).updateStatus(reportId, statusRequest);
        verify(reportService).updateProject(reportId, projectRequest);
        verify(reportService).updateComponent(reportId, componentRequest);
        verify(reportService).updateDescription(reportId, descriptionRequest);
        verify(reportService).updateStepsToReproduce(reportId, stepsRequest);
        verify(reportService).updateExpectedBehavior(reportId, expectedRequest);
        verify(reportService).updateActualBehavior(reportId, actualRequest);
    }

    @Test
    void updateAssigneeRejectsMissingAssigneeId() throws Exception {
        mockMvc.perform(patch("/api/reports/{reportId}/assignee", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        org.mockito.Mockito.verifyNoInteractions(reportService);
    }

    private static CreateBugReportRequest createRequest() {
        return new CreateBugReportRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Application crashes",
                "Description",
                "Steps",
                "Expected",
                "Actual",
                EBugSeverity.HIGH);
    }

    private static String createRequestJson(CreateBugReportRequest request) {
        return """
                {
                  "assigneeId": "%s",
                  "projectId": "%s",
                  "componentId": "%s",
                  "title": "Application crashes",
                  "description": "Description",
                  "stepsToReproduce": "Steps",
                  "expectedBehavior": "Expected",
                  "actualBehavior": "Actual",
                  "severity": "HIGH"
                }
                """.formatted(
                request.assigneeId(),
                request.projectId(),
                request.componentId());
    }

    private static BugReportResponse reportResponse(UUID reportId) {
        return reportResponse(reportId, null);
    }

    private static BugReportResponse reportResponse(UUID reportId, String assigneeName) {
        return new BugReportResponse(
                reportId,
                "Joe Reporter",
                assigneeName,
                "Bug Report",
                "Backend API",
                "Application crashes",
                null,
                null,
                null,
                null,
                EBugSeverity.HIGH,
                EBugStatus.OPEN,
                null,
                null,
                null);
    }

    private static BugReportBriefResponse briefResponse(UUID reportId) {
        return new BugReportBriefResponse(
                reportId,
                "Application crashes",
                "Leo Tester",
                null,
                EBugStatus.OPEN,
                EBugSeverity.HIGH,
                "13-07-2026 12:05");
    }

    private static void authenticate(UUID accountId) {
        var account = org.mockito.Mockito.mock(UserAccountDetails.class);
        when(account.getId()).thenReturn(accountId);
        var authentication = new UsernamePasswordAuthenticationToken(account, null, account.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
