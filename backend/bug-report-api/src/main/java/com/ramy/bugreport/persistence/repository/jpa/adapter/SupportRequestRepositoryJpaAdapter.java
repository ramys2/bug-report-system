package com.ramy.bugreport.persistence.repository.jpa.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ramy.bugreport.domain.ESupportRequestStatus;
import com.ramy.bugreport.domain.ESupportRequestType;
import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.SupportRequest;
import com.ramy.bugreport.persistence.mapper.SupportRequestMapper;
import com.ramy.bugreport.persistence.repository.jpa.repository.SupportRequestJpaRepository;
import com.ramy.bugreport.repository.ISupportRequestRepository;

/** Implements support storage using the same domain/JPA boundary as the other repositories. */
@Repository
@Transactional(readOnly = true)
public class SupportRequestRepositoryJpaAdapter implements ISupportRequestRepository {
    private final SupportRequestJpaRepository repository;

    public SupportRequestRepositoryJpaAdapter(SupportRequestJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<SupportRequest> findById(UUID id) {
        return repository.findById(id).map(SupportRequestMapper::toDomain);
    }

    @Override
    @Transactional
    public SupportRequest save(SupportRequest request) {
        return SupportRequestMapper.toDomain(repository.save(SupportRequestMapper.toEntity(request)));
    }

    @Override
    public boolean existsPendingRoleChange(UUID requesterId, EUserRole requestedRole) {
        return repository.existsByRequesterIdAndTypeAndRequestedRoleAndStatus(requesterId,
                ESupportRequestType.ROLE_CHANGE, requestedRole, ESupportRequestStatus.PENDING);
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
