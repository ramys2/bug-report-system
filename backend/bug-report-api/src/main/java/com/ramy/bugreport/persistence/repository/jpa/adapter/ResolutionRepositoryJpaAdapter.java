package com.ramy.bugreport.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ramy.bugreport.domain.Resolution;
import com.ramy.bugreport.persistence.mapper.ResolutionMapper;
import com.ramy.bugreport.persistence.repository.jpa.ResolutionJpaRepository;
import com.ramy.bugreport.repository.IResolutionRepository;

@Repository
@Transactional(readOnly = true)
public class ResolutionRepositoryJpaAdapter implements IResolutionRepository {
    private final ResolutionJpaRepository repository;

    public ResolutionRepositoryJpaAdapter(ResolutionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Resolution> findById(UUID id) {
        return repository.findById(id).map(ResolutionMapper::toDomain);
    }

    @Override
    public List<Resolution> findAll() {
        return repository.findAll().stream().map(ResolutionMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public Resolution save(Resolution domain) {
        return ResolutionMapper.toDomain(repository.save(ResolutionMapper.toEntity(domain)));
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
}
