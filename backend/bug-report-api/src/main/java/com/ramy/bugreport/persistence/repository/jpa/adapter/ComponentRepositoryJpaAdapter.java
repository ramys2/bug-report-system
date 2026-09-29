package com.ramy.bugreport.persistence.repository.jpa.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ramy.bugreport.domain.Component;
import com.ramy.bugreport.persistence.mapper.ComponentMapper;
import com.ramy.bugreport.persistence.repository.jpa.repository.ComponentJpaRepository;
import com.ramy.bugreport.repository.IComponentRepository;

/**
 * Implements {@link com.ramy.bugreport.repository.IComponentRepository} with Spring Data JPA: calls {@link com.ramy.bugreport.persistence.repository.jpa.repository.ComponentJpaRepository} and converts between
 * entities and domain objects with {@link com.ramy.bugreport.persistence.mapper.ComponentMapper}.
 *
 * <p>The class is read-only transactional; methods that write ({@code save}, {@code delete}, {@code deleteAll}) override this
 * with a normal transaction.
 */
@Repository
@Transactional(readOnly = true)
public class ComponentRepositoryJpaAdapter implements IComponentRepository {
    private final ComponentJpaRepository repository;

    public ComponentRepositoryJpaAdapter(ComponentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Component> findById(UUID id) {
        return repository.findById(id).map(ComponentMapper::toDomain);
    }

    @Override
    public List<Component> findAll() {
        return repository.findAll().stream().map(ComponentMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public Component save(Component domain) {
        return ComponentMapper.toDomain(repository.save(ComponentMapper.toEntity(domain)));
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
