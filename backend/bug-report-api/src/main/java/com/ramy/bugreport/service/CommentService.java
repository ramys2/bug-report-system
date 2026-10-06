package com.ramy.bugreport.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.ramy.bugreport.domain.Comment;
import com.ramy.bugreport.domain.EBugStatus;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.dto.comment.CommentResponse;
import com.ramy.bugreport.dto.comment.CreateCommentRequest;
import com.ramy.bugreport.dto.comment.CreateCommentResponse;
import com.ramy.bugreport.exception.ResourceNotFoundException;
import com.ramy.bugreport.exception.BusinessRuleConflictException;
import com.ramy.bugreport.repository.IBugReportRepository;
import com.ramy.bugreport.repository.ICommentRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;

import jakarta.transaction.Transactional;

/**
 * Business logic for comments on bug reports. Comments can be listed at any time,
 * but added or deleted only while the report is not closed.
 */
@Service
public class CommentService {
    private final ICommentRepository commentRepository;
    private final IBugReportRepository bugReportRepository;
    private final IUserAccountRepository userAccountRepository;

    public CommentService(
            ICommentRepository commentRepository,
            IBugReportRepository bugReportRepository,
            IUserAccountRepository userAccountRepository
    ) {
        this.commentRepository = commentRepository;
        this.bugReportRepository = bugReportRepository;
        this.userAccountRepository = userAccountRepository;
    }

    /*
    * ============================================
    *
    * GET
    *
    * ============================================
    */

    /**
     * Returns the comments of a report, newest first, with each author's details.
     *
     * @param reportId id of the report
     * @throws ResourceNotFoundException if the report or a comment's author does not exist
     */
    public List<CommentResponse> getComments(UUID reportId) {
        if (!bugReportRepository.existsById(reportId)) {
            throw new ResourceNotFoundException("Report with id: %s".formatted(reportId));
        }

        return commentRepository.findByBugReportId(reportId)
                .stream()
                .sorted(Comparator.comparing(Comment::getCreatedAt).reversed())
                .map(comment -> {
                    UserAccount author = userAccountRepository.findById(comment.getAuthorId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "User with id=%s does not exist!".formatted(comment.getAuthorId())
                            ));
                    return CommentResponse.from(comment, author);
                })
                .toList();
    }

    /*
    * ============================================
    *
    * POST
    *
    * ============================================
    */

    /**
     * Adds a comment to a report, timestamped with the current time.
     *
     * @param authorId id of the commenting user
     * @param request the id of the report to comment on and the comment text
     * @return the saved comment with its author
     * @throws ResourceNotFoundException if the report or the author does not exist
     * @throws BusinessRuleConflictException if the report is closed
     */
    @Transactional
    public CreateCommentResponse create(UUID authorId, CreateCommentRequest request) {
        var reportId = request.reportId();
        requireOpenReport(reportId);

        UserAccount author = userAccountRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id=%s does not exist!".formatted(authorId)
                ));

        Comment comment = new Comment(
                reportId,
                authorId,
                request.content(),
                LocalDateTime.now());
        comment = commentRepository.save(comment);

        return CreateCommentResponse.from(comment, author);
    }

    /*
    * ============================================
    *
    * DELETE
    *
    * ============================================
    */

    /**
     * Deletes a comment.
     *
     * <p>Requires the ADMIN role or being the comment's author (checked by {@code CommentAuthorizer.canDelete}).
     *
     * @param commentId id of the comment
     * @throws ResourceNotFoundException if the comment or its report does not exist
     * @throws BusinessRuleConflictException if the report is closed
     */
    @Transactional
    @PreAuthorize(
    		"hasRole('ADMIN') or @commentAuthorizer.canDelete(#commentId, authentication)"
    )
    public void delete(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment with id: %s".formatted(commentId)));

        requireOpenReport(comment.getBugReportId());
        commentRepository.delete(comment);
    }

    /** Fails unless the report exists and is not closed. */
    private void requireOpenReport(UUID reportId) {
        var report = bugReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report with id: %s".formatted(reportId)));

        if (report.getStatus() == EBugStatus.CLOSED) {
            throw new BusinessRuleConflictException("Comments cannot be changed on a closed report.");
        }
    }
}
