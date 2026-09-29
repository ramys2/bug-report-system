package com.ramy.bugreport.persistence.repository.jpa.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ramy.bugreport.domain.Resolution;
import com.ramy.bugreport.persistence.mapper.ResolutionMapper;
import com.ramy.bugreport.persistence.repository.jpa.repository.ResolutionJpaRepository;
import com.ramy.bugreport.repository.IResolutionRepository;

/**
 * Implements {@link com.ramy.bugreport.repository.IResolutionRepository} with Spring Data JPA: calls {@link com.ramy.bugreport.persistence.repository.jpa.repository.ResolutionJpaRepository} and converts between
 * entities and domain objects with {@link com.ramy.bugreport.persistence.mapper.ResolutionMapper}.
 *
 * <p>The class is read-only transactional; methods that write ({@code save}, {@code delete}, {@code deleteAll}) override this
 * with a normal transaction.
 */
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
