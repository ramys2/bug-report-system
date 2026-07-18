package com.ramy.bugreport.domain;

import java.nio.file.Path;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PathAttributeConverter implements AttributeConverter<Path, String> {

    @Override
    public String convertToDatabaseColumn(Path path) {
        return path == null ? null : path.toString();
    }

    @Override
    public Path convertToEntityAttribute(String value) {
        return value == null ? null : Path.of(value);
    }
}
