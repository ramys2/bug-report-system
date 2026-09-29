package com.ramy.bugreport.component;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.exception.ResourceNotFoundException;
import com.ramy.bugreport.repository.IBugReportRepository;
import com.ramy.bugreport.security.UserAccountDetails;

/**
 * Authorization helper used in {@code @PreAuthorize} expressions of {@code BugReportService}
 * (as the bean {@code bugReportAuthorizer}).
 */
@Component
public class BugReportAuthorizer {
	
	private final IBugReportRepository reportRepository;
	
	public BugReportAuthorizer(IBugReportRepository reportRepository) {
		this.reportRepository = reportRepository;
	}
	
	/**
	 * Checks whether the signed-in user is the report's reporter or its assignee. Admins are handled separately in the expression.
	 *
	 * @param reportId id of the report
	 * @param auth the current authentication; its principal must be a {@link com.ramy.bugreport.security.UserAccountDetails}
	 * @return {@code true} if the user is the reporter or the assignee
	 * @throws ResourceNotFoundException if the report does not exist. TODO(verify): whether this reaches the client as 404, since it is thrown while evaluating a security expression
	 */
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
