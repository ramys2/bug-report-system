package com.ramy.bugreport.persistence.repository.jpa.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ramy.bugreport.persistence.entity.CommentEntity;

/** Spring Data repository for {@link com.ramy.bugreport.persistence.entity.CommentEntity}. */
public interface CommentJpaRepository extends JpaRepository<CommentEntity, UUID> {
    /** Comments on the given report. */
    List<CommentEntity> findByBugReportId(UUID bugReportId);
}
