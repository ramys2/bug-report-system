package com.ramy.bugreport.domain;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolution
 */
public record Resolution(
    UUID id,
    UUID bugReportId,
    String description,
    LocalDateTime resolvedAt,
    Optional<String> fixedVersion,
    Optional<String> commitUrl
) {

}
