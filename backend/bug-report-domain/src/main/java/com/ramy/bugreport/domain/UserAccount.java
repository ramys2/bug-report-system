package com.ramy.bugreport.domain;
import java.util.UUID;

public class UserAccount {

    private UUID id;
    private String name;

    private String emailAddress;

    private String passwordHash;

    private EUserRole role;

    public UserAccount(String name, String emailAddress, String passwordHash, EUserRole role) {
        this(null, name, emailAddress, passwordHash, role);
    }

    public UserAccount(UUID id, String name, String emailAddress, String passwordHash, EUserRole role) {
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
    public void setRole(EUserRole newRole) {
        this.role = newRole;
    }

    @Override
    public String toString() {
        return "User[id=%s, name=%s, email=%s, role=%s]".formatted(id, name, emailAddress, role);
    }
}
