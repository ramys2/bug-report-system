package com.ramy.bugreport.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.ramy.bugreport.service.CommentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/reports/{reportId}/comments")
    public List<CommentResponse> getComments(
            @PathVariable UUID reportId
    ) {
        return commentService.getComments(reportId);
    }

    @PostMapping("/reports/{reportId}/comments")
    public ResponseEntity<CreateCommentResponse> create(
            @PathVariable UUID reportId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        var response = commentService.create(reportId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID commentId
    ) {
        commentService.delete(commentId);
        return ResponseEntity.noContent().build();
    }
}
