package com.ramy.bugreport.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ramy.bugreport.dto.comment.CommentResponse;
import com.ramy.bugreport.dto.comment.CreateCommentRequest;
import com.ramy.bugreport.dto.comment.CreateCommentResponse;
import com.ramy.bugreport.security.UserAccountDetails;
import com.ramy.bugreport.service.CommentService;

import jakarta.validation.Valid;

/**
 * REST endpoints for comments on bug reports: {@code /api/reports/{reportId}/comments} and {@code /api/comments/{commentId}}.
 *
 * <p>Errors are returned as JSON {@code {"message": "..."}} ({@link com.ramy.bugreport.exception.ApiErrorResponse}):
 * 400 for invalid input, 401 when not signed in, 403 when the role or ownership check fails,
 * 404 when a referenced resource does not exist, 409 for business rule conflicts. Requests that
 * change data ({@code POST}, {@code PATCH}, {@code DELETE}) also need a CSRF token, see {@code GET /api/csrf}.
 */
@RestController
@RequestMapping("/api")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * {@code GET /api/reports/{reportId}/comments}: lists the comments of a report, newest first.
     *
     * <p>Access: any signed-in user.
     *
     * @param reportId id of the report
     * @return 200 with a list of {@code {id, bugReportId, authorId, authorName, content, createdAt}}; empty if there are none
     * @throws ResourceNotFoundException 404 if the report does not exist
     */
    @GetMapping("/reports/{reportId}/comments")
    public List<CommentResponse> getComments(
            @PathVariable UUID reportId
    ) {
        return commentService.getComments(reportId);
    }

    /**
     * {@code POST /api/reports/{reportId}/comments}: adds a comment as the signed-in user.
     *
     * <p>Access: any signed-in user.
     *
     * @param reportId id of the report
     * @param request body {@code {content}}, required and not blank
     * @return 201 with {@code {id, authorId, authorName, content, createdAt}}
     * @throws ResourceNotFoundException 404 if the report does not exist
     * @throws BusinessRuleConflictException 409 if the report is closed
     */
    @PostMapping("/reports/{reportId}/comments")
    public ResponseEntity<CreateCommentResponse> create(
            @PathVariable UUID reportId,
            @AuthenticationPrincipal UserAccountDetails account,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        var response = commentService.create(reportId, account.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * {@code DELETE /api/comments/{commentId}}: deletes a comment.
     *
     * <p>Access: signed-in user; the service further requires the ADMIN role or being the comment's author
     * (so the "administrative action" comment in {@code SecurityConfig} is stricter than what is implemented).
     *
     * @param commentId id of the comment
     * @return 204 with no body
     * @throws ResourceNotFoundException 404 if the comment or its report does not exist
     * @throws BusinessRuleConflictException 409 if the report is closed
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID commentId
    ) {
        commentService.delete(commentId);
        return ResponseEntity.noContent().build();
    }
}
