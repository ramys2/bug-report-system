package com.ramy.bugreport.component;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.exception.ResourceNotFoundException;
import com.ramy.bugreport.repository.IBugReportRepository;
import com.ramy.bugreport.security.UserAccountDetails;

@Component
public class BugReportAuthorizer {
	
	private final IBugReportRepository reportRepository;
	
	public BugReportAuthorizer(IBugReportRepository reportRepository) {
		this.reportRepository = reportRepository;
	}
	
	public boolean canUpdate(UUID reportId, Authentication auth) {
        BugReport report = this.reportRepository.findById(reportId)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Report with id: %s".formatted(reportId)
                    )
                );
        
        UserAccountDetails account = (UserAccountDetails) auth.getPrincipal();
        
        return account.getId().equals(report.getReporterId()) ||
        		(report.getAssigneeId() != null && report.getAssigneeId().equals(account.getId()));
	}
	
}
