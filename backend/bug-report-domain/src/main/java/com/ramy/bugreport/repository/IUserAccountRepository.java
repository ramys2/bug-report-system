package com.ramy.bugreport.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.UserAccount;

public interface IUserAccountRepository {
    Optional<UserAccount> findById(UUID id);
    List<UserAccount> findAll();
    List<UserAccount> findAllById(Iterable<UUID> ids);
    UserAccount save(UserAccount account);
    boolean existsById(UUID id);
    long count();
    void deleteAll();
    Optional<UserAccount> findByEmailAddress(String emailAddress);
    boolean existsByEmailAddress(String emailAddress);
    List<UserAccount> findByNameContainingIgnoreCase(String name);

    /** Locks matching accounts until the calling transaction completes. */
    List<UserAccount> findAllByRole(EUserRole role);
}
