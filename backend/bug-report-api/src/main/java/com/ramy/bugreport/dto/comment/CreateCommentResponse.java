package com.ramy.bugreport.dto.comment;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ramy.bugreport.domain.Comment;
import com.ramy.bugreport.domain.UserAccount;
/**
 * Response of {@code POST /api/reports/{reportId}/comments}: the saved comment.
 *
 * @param id id of the new comment
 * @param authorId id of the author
 * @param authorName display name of the author
 * @param content comment text
 * @param createdAt when the comment was written (ISO-8601)
 */
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
