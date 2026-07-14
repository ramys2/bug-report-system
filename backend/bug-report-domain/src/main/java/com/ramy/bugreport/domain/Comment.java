package com.ramy.bugreport.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class Comment {
    private UUID id;
    private UUID bugReportId;
    private UUID authorId;
    private String content;
    private LocalDateTime createdAt;

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
