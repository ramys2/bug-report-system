package com.ramy.bugreport.dto.report;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ramy.bugreport.domain.Resolution;
import com.ramy.bugreport.openapi.ApiExamples;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resolution details included in a report response.
 *
 * @param id resolution id
 * @param description what was done to fix the bug
 * @param resolvedAt when the bug was resolved (ISO-8601)
 * @param fixedVersion version containing the fix; may be null
 * @param commitUrl URL of the fixing commit; may be null
 */
@Schema(description = "Resolution details included in a report response.")
public record ResolutionResponse(
        @Schema(description = "Resolution id.", example = ApiExamples.UUID)
        UUID id,
        @Schema(description = "What was done to fix the bug.", example = "Handle an absent optional avatar before constructing the user response.")
        String description,
        @Schema(description = "When the bug was resolved (ISO-8601).", example = ApiExamples.DATE_TIME)
        LocalDateTime resolvedAt,
        @Schema(description = "Version containing the fix. May be null.", example = "0.1.1")
        String fixedVersion,
        @Schema(description = "URL of the fixing commit. May be null.", example = "https://example.invalid/commits/7a21c9d")
        String commitUrl
) {

    public static ResolutionResponse from(Resolution resolution) {
        return new ResolutionResponse(
                resolution.getId(),
                resolution.getDescription(),
                resolution.getResolvedAt(),
                resolution.getFixedVersion(),
                resolution.getCommitUrl());
    }
}
