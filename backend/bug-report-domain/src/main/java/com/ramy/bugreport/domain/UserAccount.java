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
     * Unique username, also used as the display name.
     */
    private String name;

    /**
     * Email address; unique across accounts (enforced by the database).
     */
    private String emailAddress;

    /**
     * BCrypt hash of the password, or null if password login is unavailable; never the plain text (hashed with the
     * {@code PasswordEncoder} bean defined in {@code SecurityConfig}).
     */
    private String passwordHash;

    /** Stable, case-sensitive Google subject; null for accounts not registered through Google. */
    private String googleSubject;

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
        this(id, name, emailAddress, passwordHash, role, null);
    }

    /** A null password hash means that password login is unavailable. */
    public UserAccount(UUID id, String name, String emailAddress, String passwordHash, EUserRole role,
            String googleSubject) {
        this.id = id;
        this.name = name;
        this.emailAddress = emailAddress;
        this.passwordHash = passwordHash;
        this.role = role;
        this.googleSubject = googleSubject;
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

    public String getGoogleSubject() {
        return googleSubject;
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
