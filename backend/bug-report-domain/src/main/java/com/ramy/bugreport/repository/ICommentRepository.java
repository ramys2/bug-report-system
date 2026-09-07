package com.ramy.bugreport.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ramy.bugreport.domain.Comment;

public interface ICommentRepository {
    Optional<Comment> findById(UUID id);
    List<Comment> findAll();
    Comment save(Comment domain);
    long count();
    void deleteAll();
    void delete(Comment comment);
    List<Comment> findByBugReportId(UUID bugReportId);
}
