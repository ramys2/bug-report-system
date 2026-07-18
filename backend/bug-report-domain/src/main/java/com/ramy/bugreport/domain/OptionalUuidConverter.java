package com.ramy.bugreport.domain;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class OptionalUuidConverter implements AttributeConverter<Optional<UUID>, UUID> {

    @Override
    public UUID convertToDatabaseColumn(Optional<UUID> value) {
        return value.orElse(null);
    }

    @Override
    public Optional<UUID> convertToEntityAttribute(UUID value) {
        return Optional.ofNullable(value);
    }
}
