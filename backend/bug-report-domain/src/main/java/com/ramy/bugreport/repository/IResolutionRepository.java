package com.ramy.bugreport.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ramy.bugreport.domain.Resolution;

public interface IResolutionRepository {
    Optional<Resolution> findById(UUID id);
    List<Resolution> findAll();
    Resolution save(Resolution domain);
    long count();
    void deleteAll();
}
