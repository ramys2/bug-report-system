package com.ramy.bugreport.dto.comment;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ramy.bugreport.domain.Comment;
import com.ramy.bugreport.domain.UserAccount;
/**
 * A comment in {@code GET /api/reports/{reportId}/comments}.
 *
 * @param id comment id
 * @param bugReportId id of the commented report
 * @param authorId id of the author
 * @param authorName display name of the author
 * @param content comment text
 * @param createdAt when the comment was written (ISO-8601)
 */
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
