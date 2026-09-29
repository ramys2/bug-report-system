package com.ramy.bugreport.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.UserAccount;

/** Storage contract for {@link UserAccount}s. Implemented by a JPA adapter in the API module. */
public interface IUserAccountRepository {
    /**
     * Finds one user account by id.
     *
     * @return the user account, or empty if none exists
     */
    Optional<UserAccount> findById(UUID id);
    /** Returns all stored user account objects, in no guaranteed order. */
    List<UserAccount> findAll();
    /** Returns the accounts with the given ids. Ids that do not exist are silently skipped. */
    List<UserAccount> findAllById(Iterable<UUID> ids);
    /**
     * Inserts the object if its id is {@code null}, otherwise updates the stored object with that id.
     *
     * @param account the object to store
     * @return the stored object, with its generated id filled in
     */
    UserAccount save(UserAccount account);
    /** Returns whether an account with this id exists. */
    boolean existsById(UUID id);
    /** Returns the number of stored user account objects. */
    long count();
    /** Removes every stored user account. Mainly intended for test setup. */
    void deleteAll();
    /**
     * Finds an account by its exact email address.
     *
     * @return the account, or empty if none has this address
     */
    Optional<UserAccount> findByEmailAddress(String emailAddress);
    /** Returns whether an account with this exact email address exists. */
    boolean existsByEmailAddress(String emailAddress);
    /** Returns accounts whose name contains {@code name}, ignoring case. */
    List<UserAccount> findByNameContainingIgnoreCase(String name);

    /** Locks matching accounts until the calling transaction completes. */
    List<UserAccount> findAllByRole(EUserRole role);
}
