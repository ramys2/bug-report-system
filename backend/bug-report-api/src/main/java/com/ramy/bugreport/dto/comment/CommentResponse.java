package com.ramy.bugreport.dto.comment;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ramy.bugreport.domain.Comment;
import com.ramy.bugreport.domain.UserAccount;
public record CommentResponse(
        UUID id,
        UUID bugReportId,
        UUID authorId,
        String authorName,
        String content,
        LocalDateTime createdAt
) {

    public static CommentResponse from(Comment comment, UserAccount author) {
        return new CommentResponse(
                comment.getId(),
                comment.getBugReportId(),
                comment.getAuthorId(),
                author.getName(),
                comment.getContent(),
                comment.getCreatedAt());
    }
}
