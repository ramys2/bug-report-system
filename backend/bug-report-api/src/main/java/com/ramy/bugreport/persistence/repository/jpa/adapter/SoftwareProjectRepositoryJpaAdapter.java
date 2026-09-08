package com.ramy.bugreport.persistence.repository.jpa.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ramy.bugreport.domain.SoftwareProject;
import com.ramy.bugreport.persistence.mapper.SoftwareProjectMapper;
import com.ramy.bugreport.persistence.repository.jpa.repository.SoftwareProjectJpaRepository;
import com.ramy.bugreport.repository.ISoftwareProjectRepository;

@Repository
@Transactional(readOnly = true)
public class SoftwareProjectRepositoryJpaAdapter implements ISoftwareProjectRepository {
    private final SoftwareProjectJpaRepository repository;

    public SoftwareProjectRepositoryJpaAdapter(SoftwareProjectJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<SoftwareProject> findById(UUID id) {
        return repository.findById(id).map(SoftwareProjectMapper::toDomain);
    }

    @Override
    public List<SoftwareProject> findAll() {
        return repository.findAll().stream().map(SoftwareProjectMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public SoftwareProject save(SoftwareProject domain) {
        return SoftwareProjectMapper.toDomain(repository.save(SoftwareProjectMapper.toEntity(domain)));
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    @Transactional
    public void deleteAll() {
        repository.deleteAll();
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }
}
