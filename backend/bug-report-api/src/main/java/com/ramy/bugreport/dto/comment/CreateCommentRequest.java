package com.ramy.bugreport.dto.comment;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body of {@code POST /api/reports/{reportId}/comments}.
 *
 * @param content comment text; required, not blank
 */
@Schema(description = "Request body of `POST /api/reports/{reportId}/comments`.")
public record CreateCommentRequest(
        @Schema(description = "Comment text.", example = "This also happens in a private browser window.")
        @NotBlank String content
) {
}
