package com.ramy.bugreport.dto.report;

import com.ramy.bugreport.domain.EBugStatus;
import com.ramy.bugreport.domain.Resolution;
import java.util.UUID;


public record UpdateBugReportRequest(
        UUID assigneeId,
        String description,
        String stepsToReproduce,
        String expectedBehavior,
        String actualBehavior,
        EBugStatus bugStatus
) {

}
