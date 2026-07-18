package com.ramy.bugreport.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ramy.bugreport.dto.report.BugReportResponse;
import com.ramy.bugreport.dto.report.CreateBugReportRequest;
import com.ramy.bugreport.dto.report.CreateBugReportResponse;
import com.ramy.bugreport.service.BugReportService;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/api/reports")
public class BugReportController {
    private final BugReportService reportService;

    public BugReportController(BugReportService bugReportService) {
        reportService = bugReportService;
    }

    @GetMapping
    public List<BugReportResponse> getAll() {
        return reportService.getAll();
    }

    @PostMapping
    public ResponseEntity<CreateBugReportResponse> create(
        @Valid @RequestBody CreateBugReportRequest request
    ) {
        var response = reportService.create(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }
    
}
