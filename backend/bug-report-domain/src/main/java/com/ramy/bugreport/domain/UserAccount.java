package com.ramy.bugreport.domain;
import java.util.UUID;

/**
 * A registered user of the system.
 *
 * Only the role can be changed after construction.
 */
public class UserAccount {

    /**
     * Unique identifier. {@code null} until the account is saved.
     */
    private UUID id;
    /**
     * Display name.
     */
    private String name;

    /**
     * Email address; unique across accounts (enforced by the database).
     */
    private String emailAddress;

    /**
     * BCrypt hash of the password, never the plain text (hashed with the
     * {@code PasswordEncoder} bean defined in {@code SecurityConfig}).
     */
    private String passwordHash;

    /**
     * Role that determines what the user is allowed to do.
     */
    private EUserRole role;

    /**
     * Creates a new, not yet saved account ({@code id} is {@code null}).
     *
     * @param passwordHash already hashed password, not the plain text
     */
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

    /**
     * Returns id, name, email and role. The password hash is deliberately left out.
     */
    @Override
    public String toString() {
        return "User[id=%s, name=%s, email=%s, role=%s]".formatted(id, name, emailAddress, role);
    }
}
