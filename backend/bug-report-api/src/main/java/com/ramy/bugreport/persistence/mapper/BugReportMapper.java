package com.ramy.bugreport.persistence.mapper;

import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.persistence.entity.BugReportEntity;

/**
 * Converts between the domain class {@link com.ramy.bugreport.domain.BugReport} and its JPA entity {@link com.ramy.bugreport.persistence.entity.BugReportEntity}.
 * Both directions copy every field one to one (including the nested resolution, via {@link ResolutionMapper}); a {@code null} input gives a {@code null} result.
 */
public final class BugReportMapper {
    private BugReportMapper() {
    }

    public static BugReportEntity toEntity(BugReport domain) {
        if (domain == null) {
            return null;
        }
        BugReportEntity entity = new BugReportEntity();
        entity.setId(domain.getId());
        entity.setReporterId(domain.getReporterId());
        entity.setAssigneeId(domain.getAssigneeId());
        entity.setProjectId(domain.getProjectId());
        entity.setComponentId(domain.getComponentId());
        entity.setTitle(domain.getTitle());
        entity.setDescription(domain.getDescription());
        entity.setStepsToReproduce(domain.getStepsToReproduce());
        entity.setExpectedBehavior(domain.getExpectedBehavior());
        entity.setActualBehavior(domain.getActualBehavior());
        entity.setSeverity(domain.getSeverity());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setResolution(ResolutionMapper.toEntity(domain.getResolution()));
        return entity;
    }

    /**
     * Rebuilds a domain report with all stored values supplied through the builder.
     */
    public static BugReport toDomain(BugReportEntity entity) {
        if (entity == null) {
            return null;
        }
        return BugReport.builder(entity.getReporterId(), entity.getProjectId(),
                        entity.getComponentId(), entity.getTitle(), entity.getSeverity())
                .id(entity.getId())
                .status(entity.getStatus())
                .resolution(ResolutionMapper.toDomain(entity.getResolution()))
                .assigneeId(entity.getAssigneeId())
                .description(entity.getDescription())
                .stepsToReproduce(entity.getStepsToReproduce())
                .expectedBehavior(entity.getExpectedBehavior())
                .actualBehavior(entity.getActualBehavior())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
