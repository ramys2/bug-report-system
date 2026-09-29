package com.ramy.bugreport.domain;
/**
 * Role of a {@link UserAccount}, which determines what the user may do. Stored by name in the database.
 */
public enum EUserRole {
    REPORTER,
    DEVELOPER,
    ADMIN
}
