package com.ramy.bugreport.dto.comment;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ramy.bugreport.domain.Comment;

public record CommentResponse(
        UUID id,
        UUID bugReportId,
        UUID authorId,
        String content,
        LocalDateTime createdAt
) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getBugReportId(),
                comment.getAuthorId(),
                comment.getContent(),
                comment.getCreatedAt());
    }
}
