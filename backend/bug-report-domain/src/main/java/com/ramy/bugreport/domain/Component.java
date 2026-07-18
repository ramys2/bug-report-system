package com.ramy.bugreport.domain;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Component {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Convert(converter = OptionalUuidConverter.class)
    private Optional<UUID> responsibleDeveloperId = Optional.empty();

    protected Component() {
        // Required by JPA
    }

    public Component(String name, String description, Optional<UUID> responsibleDeveloperId) {
        this.name = name;
        this.description = description;
        this.responsibleDeveloperId = responsibleDeveloperId;
    }

    public UUID  getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Optional<UUID> getResponsibleDeveloperId() {
        return responsibleDeveloperId;
    }

    public void setResponsibleDeveloperId(UUID responsibleDeveloperId) {
        this.responsibleDeveloperId = Optional.ofNullable(responsibleDeveloperId);
    }

}
