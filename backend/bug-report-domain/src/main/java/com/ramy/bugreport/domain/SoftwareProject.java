package com.ramy.bugreport.domain;

import java.util.UUID;

/**
 * A software project that bug reports are filed against.
 */
public class SoftwareProject {

    /**
     * Unique identifier. {@code null} until the project is saved.
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
     * Creates a new, not yet saved project ({@code id} is {@code null}).
     */
    public SoftwareProject(String name, String description) {
        this(null, name, description);
    }

    public SoftwareProject(UUID id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public UUID getId() {
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
}
