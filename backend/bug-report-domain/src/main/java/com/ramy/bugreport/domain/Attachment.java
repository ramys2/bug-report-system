package com.ramy.bugreport.domain;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

// @Entity
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID bugReportId;

    @Column(nullable = false)
    private UUID uploaderId;

    private String fileName;
    private String contentType;

    @Convert(converter = PathAttributeConverter.class)
    private Path storagePath;

    private LocalDateTime uploadedAt;

    protected Attachment() {
        // Required by JPA
    }

    public Attachment(UUID bugReportId, UUID uploaderId, String fileName, String contentType, Path storagePath, LocalDateTime uploadedAt) {
        this.bugReportId = bugReportId;
        this.uploaderId = uploaderId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.storagePath = storagePath;
        this.uploadedAt = uploadedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBugReportId() {
        return bugReportId;
    }

    public UUID getUploaderId() {
        return uploaderId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public Path getStoragePath() {
        return storagePath;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
}
