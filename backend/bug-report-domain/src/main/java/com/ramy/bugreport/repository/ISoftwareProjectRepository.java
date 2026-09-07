package com.ramy.bugreport.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ramy.bugreport.domain.SoftwareProject;

public interface ISoftwareProjectRepository {
    Optional<SoftwareProject> findById(UUID id);
    List<SoftwareProject> findAll();
    SoftwareProject save(SoftwareProject domain);
    long count();
    void deleteAll();
    boolean existsById(UUID id);
}
