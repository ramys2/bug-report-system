package com.ramy.bugreport.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.ramy.bugreport.domain.Comment;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.dto.comment.CommentResponse;
import com.ramy.bugreport.dto.comment.CreateCommentRequest;
import com.ramy.bugreport.dto.comment.CreateCommentResponse;
import com.ramy.bugreport.exception.ResourceNotFoundException;
import com.ramy.bugreport.repository.IBugReportRepository;
import com.ramy.bugreport.repository.ICommentRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;

import jakarta.transaction.Transactional;

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

    public List<CommentResponse> getComments(UUID reportId) {
        if (!bugReportRepository.existsById(reportId)) {
            throw new ResourceNotFoundException("Report with id: %s".formatted(reportId));
        }

        return commentRepository.findByBugReportId(reportId)
                .stream()
                .map(CommentResponse::from)
                .toList();
    }

    /*
    * ============================================
    *
    * POST
    *
    * ============================================
    */

    @Transactional
    public CreateCommentResponse create(UUID reportId, UUID authorId, CreateCommentRequest request) {
        if (!bugReportRepository.existsById(reportId)) {
            throw new ResourceNotFoundException("Report with id: %s".formatted(reportId));
        }

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

    @Transactional
    @PreAuthorize(
    		"hasRole('ADMIN') or @commentAuthorizer.canDelete(#commentId, authentication)"
    )
    public void delete(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment with id: %s".formatted(commentId)));

        commentRepository.delete(comment);
    }
}
