package com.ramy.bugreport.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID bugReportId;

    @Column(nullable = false)
    private UUID authorId;

    private String content;
    private LocalDateTime createdAt;

    protected Comment() {
        // Required by JPA
    }

    public Comment(UUID bugReportId, UUID authorId, String content, LocalDateTime createdAt) {
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
