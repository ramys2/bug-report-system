package com.ramy.bugreport.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A comment written by a user on a {@link BugReport}. Immutable after construction.
 */
public class Comment {

    /**
     * Unique identifier. {@code null} until the comment is saved.
     */
    private UUID id;

    /**
     * Id of the commented {@link BugReport}.
     */
    private UUID bugReportId;

    /**
     * Id of the {@link UserAccount} who wrote the comment.
     */
    private UUID authorId;

    /**
     * Comment text.
     */
    private String content;
    /**
     * When the comment was written.
     */
    private LocalDateTime createdAt;

    /**
     * Creates a new, not yet saved comment ({@code id} is {@code null}).
     */
    public Comment(UUID bugReportId, UUID authorId, String content, LocalDateTime createdAt) {
        this(null, bugReportId, authorId, content, createdAt);
    }

    public Comment(UUID id, UUID bugReportId, UUID authorId, String content, LocalDateTime createdAt) {
        this.id = id;
        this.bugReportId = bugReportId;
        this.authorId = authorId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBugReportId() {
        return bugReportId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

}
