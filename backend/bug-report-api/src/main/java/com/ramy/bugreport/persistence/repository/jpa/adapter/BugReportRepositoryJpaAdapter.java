package com.ramy.bugreport.persistence.repository.jpa.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.domain.EBugStatus;
import com.ramy.bugreport.persistence.mapper.BugReportMapper;
import com.ramy.bugreport.persistence.repository.jpa.repository.BugReportJpaRepository;
import com.ramy.bugreport.repository.IBugReportRepository;

/**
 * Implements {@link com.ramy.bugreport.repository.IBugReportRepository} with Spring Data JPA: calls {@link com.ramy.bugreport.persistence.repository.jpa.repository.BugReportJpaRepository} and converts between
 * entities and domain objects with {@link com.ramy.bugreport.persistence.mapper.BugReportMapper}.
 *
 * <p>The class is read-only transactional; methods that write ({@code save}, {@code delete}, {@code deleteAll}) override this
 * with a normal transaction.
 */
@Repository
@Transactional(readOnly = true)
public class BugReportRepositoryJpaAdapter implements IBugReportRepository {
    private final BugReportJpaRepository repository;

    public BugReportRepositoryJpaAdapter(BugReportJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<BugReport> findById(UUID id) {
        return repository.findById(id).map(BugReportMapper::toDomain);
    }

    @Override
    public List<BugReport> findAll() {
        return repository.findAll().stream().map(BugReportMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public BugReport save(BugReport domain) {
        return BugReportMapper.toDomain(repository.save(BugReportMapper.toEntity(domain)));
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

    @Override
    public List<BugReport> findByReporterId(UUID reporterId) {
        return repository.findByReporterId(reporterId).stream().map(BugReportMapper::toDomain).toList();
    }

    @Override
    public List<BugReport> findByAssigneeId(UUID assigneeId) {
        return repository.findByAssigneeId(assigneeId).stream().map(BugReportMapper::toDomain).toList();
    }

    @Override
    public boolean existsByAssigneeIdAndStatusNot(UUID assigneeId, EBugStatus status) {
        return repository.existsByAssigneeIdAndStatusNot(assigneeId, status);
    }
}
