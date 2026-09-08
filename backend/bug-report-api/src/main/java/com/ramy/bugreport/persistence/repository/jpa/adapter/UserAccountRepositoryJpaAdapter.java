package com.ramy.bugreport.persistence.repository.jpa.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.persistence.mapper.UserAccountMapper;
import com.ramy.bugreport.persistence.repository.jpa.repository.UserAccountJpaRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;

@Repository
@Transactional(readOnly = true)
public class UserAccountRepositoryJpaAdapter implements IUserAccountRepository {
    private final UserAccountJpaRepository repository;

    public UserAccountRepositoryJpaAdapter(UserAccountJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<UserAccount> findById(UUID id) {
        return repository.findById(id).map(UserAccountMapper::toDomain);
    }

    @Override
    public List<UserAccount> findAll() {
        return repository.findAll().stream().map(UserAccountMapper::toDomain).toList();
    }

    @Override
    public List<UserAccount> findAllById(Iterable<UUID> ids) {
        return repository.findAllById(ids).stream().map(UserAccountMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public UserAccount save(UserAccount account) {
        return UserAccountMapper.toDomain(repository.save(UserAccountMapper.toEntity(account)));
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
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
    public Optional<UserAccount> findByEmailAddress(String emailAddress) {
        return repository.findByEmailAddress(emailAddress).map(UserAccountMapper::toDomain);
    }

    @Override
    public boolean existsByEmailAddress(String emailAddress) {
        return repository.existsByEmailAddress(emailAddress);
    }

    @Override
    public List<UserAccount> findByNameContainingIgnoreCase(String name) {
        return repository.findByNameContainingIgnoreCase(name).stream()
                .map(UserAccountMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public List<UserAccount> findAllByRole(EUserRole role) {
        return repository.findAllByRole(role).stream().map(UserAccountMapper::toDomain).toList();
    }
}
