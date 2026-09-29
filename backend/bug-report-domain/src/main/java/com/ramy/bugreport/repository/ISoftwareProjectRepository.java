package com.ramy.bugreport.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ramy.bugreport.domain.SoftwareProject;

/** Storage contract for {@link SoftwareProject}s. Implemented by a JPA adapter in the API module. */
public interface ISoftwareProjectRepository {
    /**
     * Finds one project by id.
     *
     * @return the project, or empty if none exists
     */
    Optional<SoftwareProject> findById(UUID id);
    /** Returns all stored project objects, in no guaranteed order. */
    List<SoftwareProject> findAll();
    /**
     * Inserts the object if its id is {@code null}, otherwise updates the stored object with that id.
     *
     * @param domain the object to store
     * @return the stored object, with its generated id filled in
     */
    SoftwareProject save(SoftwareProject domain);
    /** Returns the number of stored project objects. */
    long count();
    /** Removes every stored project. Mainly intended for test setup. */
    void deleteAll();
    /** Returns whether a project with this id exists. */
    boolean existsById(UUID id);
}
