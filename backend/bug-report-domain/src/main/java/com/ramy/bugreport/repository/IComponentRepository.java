package com.ramy.bugreport.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ramy.bugreport.domain.Component;

/** Storage contract for {@link Component}s. Implemented by a JPA adapter in the API module. */
public interface IComponentRepository {
    /**
     * Finds one component by id.
     *
     * @return the component, or empty if none exists
     */
    Optional<Component> findById(UUID id);
    /** Returns all stored component objects, in no guaranteed order. */
    List<Component> findAll();
    /** Returns the components that belong to the project, in no guaranteed order; empty if there are none. */
    List<Component> findByProjectId(UUID projectId);
    /**
     * Inserts the object if its id is {@code null}, otherwise updates the stored object with that id.
     *
     * @param domain the object to store
     * @return the stored object, with its generated id filled in
     */
    Component save(Component domain);
    /** Returns the number of stored component objects. */
    long count();
    /** Removes every stored component. Mainly intended for test setup. */
    void deleteAll();
    /** Returns whether a component with this id exists. */
    boolean existsById(UUID id);
}
