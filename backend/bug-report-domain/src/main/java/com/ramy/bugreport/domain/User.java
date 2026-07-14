package com.ramy.bugreport.domain;
import java.util.UUID;

public class User {
    private UUID id;
    private String name;
    private String emailAddress;
    private String passwordHash;
    private EUserRole role;

    public User(UUID id, String name, String emailAddress, String passwordHash, EUserRole role) {
        this.id = id;
        this.name = name;
        this.emailAddress = emailAddress;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public EUserRole getRole() {
        return role;
    }
}
