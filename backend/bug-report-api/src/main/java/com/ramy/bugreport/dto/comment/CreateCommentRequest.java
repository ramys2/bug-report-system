package com.ramy.bugreport.dto.comment;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body of {@code POST /api/reports/{reportId}/comments}.
 *
 * @param content comment text; required, not blank
 */
public record CreateCommentRequest(
        @NotBlank String content
) {
}
