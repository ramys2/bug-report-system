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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ramy.bugreport.dto.comment.CommentResponse;
import com.ramy.bugreport.dto.comment.CreateCommentRequest;
import com.ramy.bugreport.dto.comment.CreateCommentResponse;
import com.ramy.bugreport.security.UserAccountDetails;
import com.ramy.bugreport.service.CommentService;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.ramy.bugreport.openapi.ApiExamples;
import com.ramy.bugreport.openapi.BadRequestResponse;
import com.ramy.bugreport.openapi.UnauthorizedResponse;
import com.ramy.bugreport.openapi.ForbiddenResponse;
import com.ramy.bugreport.openapi.NotFoundResponse;
import com.ramy.bugreport.openapi.ConflictResponse;

/**
 * REST endpoints for comments on bug reports: everything is under {@code /api/comments}.
 *
 * <p>Errors are returned as JSON {@code {"message": "..."}} ({@link com.ramy.bugreport.exception.ApiErrorResponse}):
 * 400 for invalid input, 401 when not signed in, 403 when the role or ownership check fails,
 * 404 when a referenced resource does not exist, 409 for business rule conflicts. Requests that
 * change data ({@code POST}, {@code PATCH}, {@code DELETE}) also need a CSRF token, see {@code GET /api/csrf}.
 */
@Tag(name = "Comments")
@RestController
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * {@code GET /api/comments?reportId=...}: lists the comments of a report, newest first.
     *
     * <p>Access: any signed-in user.
     *
     * @param reportId id of the report; required
     * @return 200 with a list of {@code {id, reportId, authorId, authorName, content, createdAt}}; empty if there are none
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if the report does not exist
     */
    @Operation(summary = "List the comments of a report", description = "Newest first; empty if there are none. Access: any signed-in user.")
    @ApiResponse(responseCode = "200", description = "Comments of the report, newest first.")
    @BadRequestResponse
    @UnauthorizedResponse
    @NotFoundResponse
    @GetMapping
    public List<CommentResponse> getComments(
            @Parameter(description = "Id of the bug report.", example = ApiExamples.UUID, required = true) @RequestParam UUID reportId
    ) {
        return commentService.getComments(reportId);
    }

    /**
     * {@code POST /api/comments}: adds a comment as the signed-in user.
     *
     * <p>Access: any signed-in user.
     *
     * @param request body {@code {reportId, content}}; both required, {@code content} not blank
     * @return 201 with {@code {id, authorId, authorName, content, createdAt}}
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if the report does not exist
     * @throws com.ramy.bugreport.exception.BusinessRuleConflictException 409 if the report is closed
     */
    @Operation(summary = "Add a comment", description = "Adds a comment as the signed-in user. Returns 409 if the report is closed. Access: any signed-in user.")
    @ApiResponse(responseCode = "201", description = "Comment created.")
    @BadRequestResponse
    @UnauthorizedResponse
    @ForbiddenResponse
    @NotFoundResponse
    @ConflictResponse
    @PostMapping
    public ResponseEntity<CreateCommentResponse> create(
            @AuthenticationPrincipal UserAccountDetails account,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        var response = commentService.create(account.getId(), request);
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
     * @throws com.ramy.bugreport.exception.ResourceNotFoundException 404 if the comment or its report does not exist
     * @throws com.ramy.bugreport.exception.BusinessRuleConflictException 409 if the report is closed
     */
    @Operation(summary = "Delete a comment", description = "Access: signed-in user; the service further requires the ADMIN role or being the comment's author. Returns 409 if the report is closed.")
    @ApiResponse(responseCode = "204", description = "Comment deleted.")
    @BadRequestResponse
    @UnauthorizedResponse
    @ForbiddenResponse
    @NotFoundResponse
    @ConflictResponse
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Id of the comment.", example = ApiExamples.UUID) @PathVariable UUID commentId
    ) {
        commentService.delete(commentId);
        return ResponseEntity.noContent().build();
    }
}
