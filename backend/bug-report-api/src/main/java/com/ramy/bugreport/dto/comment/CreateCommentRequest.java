package com.ramy.bugreport.dto.comment;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCommentRequest(
        @NotNull UUID authorId,
        @NotBlank String content
) {
}
