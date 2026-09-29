package com.ramy.bugreport.domain;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * File attached to a bug report, for example a screenshot.
 *
 * Stores only metadata; the file itself lives on disk at {@link #getStoragePath()}.
 *
 * Note: this class is not persisted yet. No entity, repository or REST endpoint uses it,
 * and the demo seeding code that creates attachments is commented out.
 */
public class Attachment {

    /**
     * Unique identifier. {@code null} until the attachment is saved.
     */
    private UUID id;

    /**
     * Id of the {@link BugReport} this file belongs to.
     */
    private UUID bugReportId;

    /**
     * Id of the {@link UserAccount} who uploaded the file.
     */
    private UUID uploaderId;

    /**
     * Original file name, as shown to users.
     */
    private String fileName;
    /**
     * MIME type of the file, e.g. {@code image/png}.
     */
    private String contentType;

    /**
     * Location of the stored file on the file system.
     */
    private Path storagePath;

    /**
     * When the file was uploaded.
     */
    private LocalDateTime uploadedAt;

    /**
     * Creates a new, not yet saved attachment ({@code id} stays {@code null}).
     */
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
