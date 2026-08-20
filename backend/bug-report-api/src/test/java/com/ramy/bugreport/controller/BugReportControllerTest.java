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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ramy.bugreport.domain.EBugSeverity;
import com.ramy.bugreport.domain.EBugStatus;
import com.ramy.bugreport.dto.report.BugReportResponse;
import com.ramy.bugreport.dto.report.CloseBugReportRequest;
import com.ramy.bugreport.dto.report.CloseBugReportResponse;
import com.ramy.bugreport.dto.report.CreateBugReportRequest;
import com.ramy.bugreport.dto.report.CreateBugReportResponse;
import com.ramy.bugreport.dto.report.UpdateBugReportRequest;
import com.ramy.bugreport.dto.report.UpdateBugReportResponse;
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
        when(reportService.getAll()).thenReturn(List.of(reportResponse(reportId)));

        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reportId.toString()));

        verify(reportService).getAll();
    }

    @Test
    void getReportUsesReportIdPath() throws Exception {
        var reportId = UUID.randomUUID();
        when(reportService.getReport(reportId)).thenReturn(reportResponse(reportId));

        mockMvc.perform(get("/api/reports/{reportId}", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reportId.toString()));

        verify(reportService).getReport(reportId);
    }

    @Test
    void getReportedUsesAuthenticatedAccountId() throws Exception {
        var reporterId = UUID.randomUUID();
        var reportId = UUID.randomUUID();
        when(reportService.getReportsByReporter(reporterId))
                .thenReturn(List.of(reportResponse(reportId)));

        authenticate(reporterId);

        mockMvc.perform(get("/api/reports/reproted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reportId.toString()));

        verify(reportService).getReportsByReporter(reporterId);
    }

    @Test
    void getAssignedUsesAuthenticatedAccountId() throws Exception {
        var assigneeId = UUID.randomUUID();
        var reportId = UUID.randomUUID();
        when(reportService.getReportsByAssignee(assigneeId))
                .thenReturn(List.of(reportResponse(reportId)));

        authenticate(assigneeId);

        mockMvc.perform(get("/api/reports/assigned"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reportId.toString()));

        verify(reportService).getReportsByAssignee(assigneeId);
    }

    @Test
    void createUsesCollectionRouteAndReturnsCreated() throws Exception {
        var request = createRequest();
        var reportId = UUID.randomUUID();
        when(reportService.create(request))
                .thenReturn(new CreateBugReportResponse(reportId, "Successfully created!"));

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(reportId.toString()))
                .andExpect(jsonPath("$.message").value("Successfully created!"));

        verify(reportService).create(request);
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
    void updateUsesReportIdPathAndReturnsOk() throws Exception {
        var reportId = UUID.randomUUID();
        var assigneeId = UUID.randomUUID();
        var request = new UpdateBugReportRequest(
                assigneeId,
                "Updated",
                "Steps",
                "Expected",
                "Actual",
                EBugStatus.IN_PROGRESS);
        when(reportService.update(reportId, request))
                .thenReturn(new UpdateBugReportResponse(
                        reportId, "Bug report updated successfully!"));

        mockMvc.perform(patch("/api/reports/{reportId}", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assigneeId": "%s",
                                  "description": "Updated",
                                  "stepsToReproduce": "Steps",
                                  "expectedBehavior": "Expected",
                                  "actualBehavior": "Actual",
                                  "bugStatus": "IN_PROGRESS"
                                }
                                """.formatted(assigneeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reportId.toString()))
                .andExpect(jsonPath("$.message").value("Bug report updated successfully!"));

        verify(reportService).update(reportId, request);
    }

    private static CreateBugReportRequest createRequest() {
        return new CreateBugReportRequest(
                UUID.randomUUID(),
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
                  "reporterId": "%s",
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
                request.reporterId(),
                request.assigneeId(),
                request.projectId(),
                request.componentId());
    }

    private static BugReportResponse reportResponse(UUID reportId) {
        return new BugReportResponse(
                reportId,
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
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

    private static void authenticate(UUID accountId) {
        var account = org.mockito.Mockito.mock(UserAccountDetails.class);
        when(account.getId()).thenReturn(accountId);
        var authentication = new UsernamePasswordAuthenticationToken(account, null, account.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
