package com.ramy.bugreport.persistence.repository.jpa.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ramy.bugreport.domain.Comment;
import com.ramy.bugreport.persistence.mapper.CommentMapper;
import com.ramy.bugreport.persistence.repository.jpa.repository.CommentJpaRepository;
import com.ramy.bugreport.repository.ICommentRepository;

/**
 * Implements {@link com.ramy.bugreport.repository.ICommentRepository} with Spring Data JPA: calls {@link com.ramy.bugreport.persistence.repository.jpa.repository.CommentJpaRepository} and converts between
 * entities and domain objects with {@link com.ramy.bugreport.persistence.mapper.CommentMapper}.
 *
 * <p>The class is read-only transactional; methods that write ({@code save}, {@code delete}, {@code deleteAll}) override this
 * with a normal transaction.
 */
@Repository
@Transactional(readOnly = true)
public class CommentRepositoryJpaAdapter implements ICommentRepository {
    private final CommentJpaRepository repository;

    public CommentRepositoryJpaAdapter(CommentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Comment> findById(UUID id) {
        return repository.findById(id).map(CommentMapper::toDomain);
    }

    @Override
    public List<Comment> findAll() {
        return repository.findAll().stream().map(CommentMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public Comment save(Comment domain) {
        return CommentMapper.toDomain(repository.save(CommentMapper.toEntity(domain)));
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
    @Transactional
    public void delete(Comment comment) {
        repository.deleteById(comment.getId());
    }

    @Override
    public List<Comment> findByBugReportId(UUID bugReportId) {
        return repository.findByBugReportId(bugReportId).stream().map(CommentMapper::toDomain).toList();
    }
}
