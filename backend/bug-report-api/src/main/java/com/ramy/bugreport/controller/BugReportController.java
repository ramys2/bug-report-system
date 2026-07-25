package com.ramy.bugreport.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ramy.bugreport.dto.report.BugReportResponse;
import com.ramy.bugreport.dto.report.CloseBugReportRequest;
import com.ramy.bugreport.dto.report.CloseBugReportResponse;
import com.ramy.bugreport.dto.report.CreateBugReportRequest;
import com.ramy.bugreport.dto.report.CreateBugReportResponse;
import com.ramy.bugreport.dto.report.UpdateBugReportRequest;
import com.ramy.bugreport.dto.report.UpdateBugReportResponse;
import com.ramy.bugreport.service.BugReportService;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;



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
    public List<BugReportResponse> getAll() {
        return reportService.getAll();
    }
    
    @GetMapping("/{reportId}")
    public BugReportResponse getReport(
            @PathVariable UUID reportId
    ) {
        return reportService.getReport(reportId);
    }
    
    @GetMapping(params = "reporterId")
    public List<BugReportResponse> getReportsByReproter(
            @RequestParam UUID reporterId
    ) {
        return reportService.getReportsByReporter(reporterId);
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
            @Valid @RequestBody CreateBugReportRequest request
    ) {
        var response = reportService.create(request);
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
    
    @PatchMapping("/{reportId}")
    public ResponseEntity<UpdateBugReportResponse> update(
            @PathVariable UUID reportId,
            @Valid @RequestBody UpdateBugReportRequest request
    ) {
        var response = reportService.update(reportId, request);
        
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
    
}
