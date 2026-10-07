package com.ramy.bugreport.repository;

import com.ramy.bugreport.domain.EUserRole;

/**
 * Conditions for finding user accounts. A {@code null} field means "no restriction"; all given fields must match.
 *
 * @param id text the account id must contain, ignoring case
 * @param name text the name must contain, ignoring case
 * @param email text the email address must contain, ignoring case
 * @param role the exact role
 */
public record UserAccountFilter(String id, String name, String email, EUserRole role) {
}
