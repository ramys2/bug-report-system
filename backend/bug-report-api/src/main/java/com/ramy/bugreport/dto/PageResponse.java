package com.ramy.bugreport.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One page of a list, as returned by paged endpoints such as {@code GET /api/accounts}.
 *
 * @param items the items on this page
 * @param page zero-based number of this page
 * @param size requested number of items per page (the last page may hold fewer)
 * @param totalElements number of items on all pages together
 * @param totalPages number of pages; 0 if there are no items
 * @param <T> type of the items
 */
@Schema(description = "One page of a list.")
public record PageResponse<T>(
        @Schema(description = "The items on this page.", requiredMode = Schema.RequiredMode.REQUIRED)
        List<T> items,
        @Schema(description = "Zero-based number of this page.", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        int page,
        @Schema(description = "Requested number of items per page.", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        int size,
        @Schema(description = "Number of items on all pages together.", example = "37", requiredMode = Schema.RequiredMode.REQUIRED)
        long totalElements,
        @Schema(description = "Number of pages; 0 if there are no items.", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
        int totalPages
) {

    public static <T> PageResponse<T> of(List<T> items, int page, int size, long totalElements) {
        int totalPages = (int) ((totalElements + size - 1) / size);
        return new PageResponse<>(items, page, size, totalElements, totalPages);
    }
}
