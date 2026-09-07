package com.ramy.bugreport.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ramy.bugreport.domain.Component;

public interface IComponentRepository {
    Optional<Component> findById(UUID id);
    List<Component> findAll();
    Component save(Component domain);
    long count();
    void deleteAll();
    boolean existsById(UUID id);
}
