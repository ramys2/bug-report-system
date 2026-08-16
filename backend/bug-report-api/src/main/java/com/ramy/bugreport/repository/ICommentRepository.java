package com.ramy.bugreport.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ramy.bugreport.domain.Comment;

public interface ICommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByBugReportId(UUID bugReportId);
}
