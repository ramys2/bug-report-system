package com.ramy.bugreport.dto.comment;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ramy.bugreport.domain.Comment;
import com.ramy.bugreport.domain.UserAccount;

public record CreateCommentResponse(
        UUID id,
        UUID authorId,
        String authorName,
        String content,
        LocalDateTime createdAt
) {

    public static CreateCommentResponse from(Comment comment, UserAccount author) {
        return new CreateCommentResponse(
                comment.getId(),
                comment.getAuthorId(),
                author.getName(),
                comment.getContent(),
                comment.getCreatedAt());
    }
}
