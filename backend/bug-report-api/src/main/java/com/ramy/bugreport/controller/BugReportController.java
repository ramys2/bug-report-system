package com.ramy.bugreport.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/api/reports")
public class BugReportController {
    private final BugReportService reportService;

    public BugReportController(BugReportService bugReportService) {
        reportService = bugReportService;
    }
    
    /*
    * ============================================
    *
    * GET Mappings
    *
    * ============================================
    */

    @GetMapping
    public List<BugReportBriefResponse> getAll() {
        return reportService.getAll();
    }
    
    @GetMapping("/{reportId}")
    public BugReportResponse getReport(
            @PathVariable UUID reportId
    ) {
        return reportService.getReport(reportId);
    }

    @GetMapping("/reported")
    public List<BugReportBriefResponse> getReported(
    		@AuthenticationPrincipal UserAccountDetails account
    ) {
        return reportService.getReportsByReporter(account.getId());
    }
    
    @GetMapping("/assigned")
    public List<BugReportBriefResponse> getAssigned(
    		@AuthenticationPrincipal UserAccountDetails account
    ) {
        return reportService.getReportsByAssignee(account.getId());
    }
    
    /*
    * ============================================
    *
    * POST Mappings
    *
    * ============================================
    */

    @PostMapping
    public ResponseEntity<CreateBugReportResponse> create(
            @Valid @RequestBody CreateBugReportRequest request,
            @AuthenticationPrincipal UserAccountDetails account
    ) {
        var response = reportService.create(account.getId(), request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }
    
    @PostMapping("/{reportId}/resolution")
    public ResponseEntity<CloseBugReportResponse> close(
            @PathVariable UUID reportId,
            @Valid @RequestBody CloseBugReportRequest request
    ) {
        var response = reportService.close(reportId, request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    
    /*
    * ============================================
    *
    * PATCH Mappings
    *
    * ============================================
    */
    
    @PatchMapping("/{reportId}/assignee")
    public ResponseEntity<UpdateBugReportResponse> updateAssignee(
            @PathVariable UUID reportId,
            @Valid @RequestBody UpdateAssigneeRequest request
    ) {
        return updateResponse(reportService.updateAssignee(reportId, request));
    }

    @PatchMapping("/{reportId}/severity")
    public ResponseEntity<UpdateBugReportResponse> updateSeverity(
            @PathVariable UUID reportId,
            @Valid @RequestBody UpdateSeverityRequest request
    ) {
        return updateResponse(reportService.updateSeverity(reportId, request));
    }

    @PatchMapping("/{reportId}/status")
    public ResponseEntity<UpdateBugReportResponse> updateStatus(
            @PathVariable UUID reportId,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        return updateResponse(reportService.updateStatus(reportId, request));
    }

    @PatchMapping("/{reportId}/project")
    public ResponseEntity<UpdateBugReportResponse> updateProject(
            @PathVariable UUID reportId,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        return updateResponse(reportService.updateProject(reportId, request));
    }

    @PatchMapping("/{reportId}/component")
    public ResponseEntity<UpdateBugReportResponse> updateComponent(
            @PathVariable UUID reportId,
            @Valid @RequestBody UpdateComponentRequest request
    ) {
        return updateResponse(reportService.updateComponent(reportId, request));
    }

    @PatchMapping("/{reportId}/description")
    public ResponseEntity<UpdateBugReportResponse> updateDescription(
            @PathVariable UUID reportId,
            @Valid @RequestBody UpdateDescriptionRequest request
    ) {
        return updateResponse(reportService.updateDescription(reportId, request));
    }

    @PatchMapping("/{reportId}/steps-to-reproduce")
    public ResponseEntity<UpdateBugReportResponse> updateStepsToReproduce(
            @PathVariable UUID reportId,
            @Valid @RequestBody UpdateStepsToReproduceRequest request
    ) {
        return updateResponse(reportService.updateStepsToReproduce(reportId, request));
    }

    @PatchMapping("/{reportId}/expected-behavior")
    public ResponseEntity<UpdateBugReportResponse> updateExpectedBehavior(
            @PathVariable UUID reportId,
            @Valid @RequestBody UpdateExpectedBehaviorRequest request
    ) {
        return updateResponse(reportService.updateExpectedBehavior(reportId, request));
    }

    @PatchMapping("/{reportId}/actual-behavior")
    public ResponseEntity<UpdateBugReportResponse> updateActualBehavior(
            @PathVariable UUID reportId,
            @Valid @RequestBody UpdateActualBehaviorRequest request
    ) {
        return updateResponse(reportService.updateActualBehavior(reportId, request));
    }

    private ResponseEntity<UpdateBugReportResponse> updateResponse(
            UpdateBugReportResponse response
    ) {
        return ResponseEntity.ok(response);
    }
    
}
