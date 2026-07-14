package com.ramy.bugreport.domain;
import java.util.Optional;
import java.util.UUID;

public class Component {
    private UUID id;
    private String name;
    private String description;
    private Optional<UUID> responsibleDeveloperId;

    public Component(UUID id, String name, String description, Optional<UUID> responsibleDeveloperId) {
        this.id = id;
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
        this.responsibleDeveloperId = Optional.of(responsibleDeveloperId);
    }
}
