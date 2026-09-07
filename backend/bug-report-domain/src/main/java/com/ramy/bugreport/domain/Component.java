package com.ramy.bugreport.domain;

import java.util.UUID;

public class Component {

    private UUID id;

    private String name;

    private String description;

    private UUID responsibleUserId;

    public Component(String name, String description, UUID responsibleUserId) {
        this(null, name, description, responsibleUserId);
    }

    public Component(UUID id, String name, String description, UUID responsibleUserId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.responsibleUserId = responsibleUserId;
    }

    public UUID  getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getResponsibleUserId() {
        return responsibleUserId;
    }

    public void setResponsibleUserId(UUID responsibleUserId) {
        this.responsibleUserId = responsibleUserId;
    }

}
