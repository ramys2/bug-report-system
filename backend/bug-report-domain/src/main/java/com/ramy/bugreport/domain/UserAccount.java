package com.ramy.bugreport.domain;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String emailAddress;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EUserRole role;

    protected UserAccount() {
        // Required by JPA
    }

    public UserAccount(String name, String emailAddress, String passwordHash, EUserRole role) {
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
