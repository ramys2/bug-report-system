package com.ramy.bugreport.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ramy.bugreport.domain.Resolution;

/** Storage contract for {@link Resolution}s. Implemented by a JPA adapter in the API module. */
public interface IResolutionRepository {
    /**
     * Finds one resolution by id.
     *
     * @return the resolution, or empty if none exists
     */
    Optional<Resolution> findById(UUID id);
    /** Returns all stored resolution objects, in no guaranteed order. */
    List<Resolution> findAll();
    /**
     * Inserts the object if its id is {@code null}, otherwise updates the stored object with that id.
     *
     * @param domain the object to store
     * @return the stored object, with its generated id filled in
     */
    Resolution save(Resolution domain);
    /** Returns the number of stored resolution objects. */
    long count();
    /** Removes every stored resolution. Mainly intended for test setup. */
    void deleteAll();
}
