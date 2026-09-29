package com.ramy.bugreport.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ramy.bugreport.domain.Comment;

/** Storage contract for {@link Comment}s. Implemented by a JPA adapter in the API module. */
public interface ICommentRepository {
    /**
     * Finds one comment by id.
     *
     * @return the comment, or empty if none exists
     */
    Optional<Comment> findById(UUID id);
    /** Returns all stored comment objects, in no guaranteed order. */
    List<Comment> findAll();
    /**
     * Inserts the object if its id is {@code null}, otherwise updates the stored object with that id.
     *
     * @param domain the object to store
     * @return the stored object, with its generated id filled in
     */
    Comment save(Comment domain);
    /** Returns the number of stored comment objects. */
    long count();
    /** Removes every stored comment. Mainly intended for test setup. */
    void deleteAll();
    /**
     * Deletes the stored comment with the same id as the given one.
     *
     * @param comment a comment whose {@code id} is not {@code null}
     */
    void delete(Comment comment);
    /** Returns all comments on the given bug report, in no guaranteed order. */
    List<Comment> findByBugReportId(UUID bugReportId);
}
