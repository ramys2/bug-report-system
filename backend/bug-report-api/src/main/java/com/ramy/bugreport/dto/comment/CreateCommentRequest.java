package com.ramy.bugreport.dto.comment;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * Request body of {@code POST /api/comments}.
 *
 * @param reportId id of the report to comment on; required
 * @param content comment text; required, not blank
 */
@Schema(description = "Request body of `POST /api/comments`.")
public record CreateCommentRequest(
        @Schema(description = "Id of the report to comment on.", example = ApiExamples.UUID)
        @NotNull UUID reportId,
        @Schema(description = "Comment text.", example = "This also happens in a private browser window.")
        @NotBlank String content
) {
}
