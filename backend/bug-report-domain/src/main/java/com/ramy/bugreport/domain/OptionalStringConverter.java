package com.ramy.bugreport.domain;

import java.util.Optional;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class OptionalStringConverter implements AttributeConverter<Optional<String>, String> {

    @Override
    public String convertToDatabaseColumn(Optional<String> value) {
        return value.orElse(null);
    }

    @Override
    public Optional<String> convertToEntityAttribute(String value) {
        return Optional.ofNullable(value);
    }
}
