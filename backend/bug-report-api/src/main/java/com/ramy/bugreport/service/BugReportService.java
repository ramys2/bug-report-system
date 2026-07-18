package com.ramy.bugreport.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ramy.bugreport.dto.report.BugReportResponse;
import com.ramy.bugreport.repository.IBugReportRepository;

@Service
public class BugReportService {
    private final IBugReportRepository bugReportRepository;

    public BugReportService(IBugReportRepository bugBugReportRepository) {
        this.bugReportRepository = bugBugReportRepository;
    }

    public List<BugReportResponse> getAll() {
        return bugReportRepository.findAll()
            .stream()
            .map(BugReportResponse::from)
            .toList();
    }
}
