package com.ramy.bugreport.domain;

import java.util.UUID;

/**
 * A part of a {@link SoftwareProject}'s codebase or product to which bug reports are assigned,
 * e.g. "Login" or "API". Each component has one responsible user.
 */
public class Component {

    /**
     * Unique identifier. {@code null} until the component is saved.
     */
    private UUID id;

    /**
     * Display name.
     */
    private String name;

    /**
     * Free-text description. May be {@code null}.
     */
    private String description;

    /**
     * Id of the {@link UserAccount} responsible for this component.
     */
    private UUID responsibleUserId;

    /**
     * Creates a new, not yet saved component ({@code id} is {@code null}).
     */
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
