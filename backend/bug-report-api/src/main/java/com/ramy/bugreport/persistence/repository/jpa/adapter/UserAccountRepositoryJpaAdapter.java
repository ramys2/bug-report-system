package com.ramy.bugreport.persistence.repository.jpa.adapter;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.persistence.entity.UserAccountEntity;
import com.ramy.bugreport.persistence.mapper.UserAccountMapper;
import com.ramy.bugreport.persistence.repository.jpa.repository.UserAccountJpaRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;
import com.ramy.bugreport.repository.PageQuery;
import com.ramy.bugreport.repository.PageResult;
import com.ramy.bugreport.repository.UserAccountFilter;

/**
 * Implements {@link com.ramy.bugreport.repository.IUserAccountRepository} with Spring Data JPA: calls {@link com.ramy.bugreport.persistence.repository.jpa.repository.UserAccountJpaRepository} and converts between
 * entities and domain objects with {@link com.ramy.bugreport.persistence.mapper.UserAccountMapper}.
 *
 * <p>The class is read-only transactional; methods that write ({@code save}, {@code delete}, {@code deleteAll}) override this
 * with a normal transaction.
 *
 * <p>{@code findAllByRole} is also a normal (non read-only) transaction, because the lock it takes needs one.
 */
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
    public PageResult<UserAccount> findPage(UserAccountFilter filter, PageQuery pageQuery) {
        Page<UserAccountEntity> page = repository.findPage(
                toLikePattern(filter.id()),
                toLikePattern(filter.name()),
                toLikePattern(filter.email()),
                filter.role(),
                PageRequest.of(pageQuery.page(), pageQuery.size()));
        return new PageResult<>(
                page.getContent().stream().map(UserAccountMapper::toDomain).toList(),
                page.getTotalElements());
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
    public List<UserAccount> findByRole(EUserRole role) {
        return repository.findByRole(role).stream().map(UserAccountMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public List<UserAccount> findAllByRole(EUserRole role) {
        return repository.findAllByRole(role).stream().map(UserAccountMapper::toDomain).toList();
    }

    /**
     * Turns the text into a lower-case LIKE pattern that finds it anywhere in a value ({@code %text%}), or returns
     * {@code null} for no restriction. The characters {@code %} and {@code _} (LIKE wildcards) and the escape
     * character {@code !} in the text are escaped, so a user's input is matched literally.
     */
    private static String toLikePattern(String text) {
        if (text == null) {
            return null;
        }
        String escaped = text.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }
}
