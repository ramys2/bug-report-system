package com.ramy.bugreport.dto.comment;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ramy.bugreport.domain.Comment;
import com.ramy.bugreport.domain.UserAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;
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
@Schema(description = "A comment in `GET /api/reports/{reportId}/comments`.")
public record CommentResponse(
        @Schema(description = "Comment id.", example = ApiExamples.UUID, requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,
        @Schema(description = "Id of the commented report.", example = ApiExamples.UUID, requiredMode = Schema.RequiredMode.REQUIRED)
        UUID bugReportId,
        @Schema(description = "Id of the author.", example = ApiExamples.UUID, requiredMode = Schema.RequiredMode.REQUIRED)
        UUID authorId,
        @Schema(description = "Display name of the author.", example = "Rachel Reporter", requiredMode = Schema.RequiredMode.REQUIRED)
        String authorName,
        @Schema(description = "Comment text.", example = "This also happens in a private browser window.", requiredMode = Schema.RequiredMode.REQUIRED)
        String content,
        @Schema(description = "When the comment was written (ISO-8601).", example = ApiExamples.DATE_TIME, requiredMode = Schema.RequiredMode.REQUIRED)
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
