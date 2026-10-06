package com.ramy.bugreport.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ramy.bugreport.dto.comment.CommentResponse;
import com.ramy.bugreport.dto.comment.CreateCommentRequest;
import com.ramy.bugreport.dto.comment.CreateCommentResponse;
import com.ramy.bugreport.service.CommentService;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CommentController(commentService))
                .setControllerAdvice(new com.ramy.bugreport.exception.ApiExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCommentsUsesCommentsRouteWithReportIdParameter() throws Exception {
        var reportId = UUID.randomUUID();
        var commentId = UUID.randomUUID();
        when(commentService.getComments(reportId))
                .thenReturn(List.of(commentResponse(commentId, reportId)));

        mockMvc.perform(get("/api/comments").param("reportId", reportId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(commentId.toString()))
                .andExpect(jsonPath("$[0].bugReportId").value(reportId.toString()))
                .andExpect(jsonPath("$[0].authorName").value("Ramy"));

        verify(commentService).getComments(reportId);
    }

    @Test
    void getCommentsRequiresReportId() throws Exception {
        mockMvc.perform(get("/api/comments"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request contains invalid values."));

        verifyNoInteractions(commentService);
    }

    @Test
    void createUsesCommentsRouteAndReturnsCreated() throws Exception {
        var reportId = UUID.randomUUID();
        var authorId = UUID.randomUUID();
        var commentId = UUID.randomUUID();
        var createdAt = LocalDateTime.now();
        var request = new CreateCommentRequest(reportId, "Working on a fix.");
        when(commentService.create(authorId, request))
                .thenReturn(new CreateCommentResponse(
                        commentId, authorId, "Ramy", request.content(), createdAt));

        authenticate(authorId);

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportId": "%s",
                                  "content": "Working on a fix."
                                }
                """.formatted(reportId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(commentId.toString()))
                .andExpect(jsonPath("$.authorId").value(authorId.toString()))
                .andExpect(jsonPath("$.authorName").value("Ramy"))
                .andExpect(jsonPath("$.content").value(request.content()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        verify(commentService).create(authorId, request);
    }

    @Test
    void createRejectsMissingReportId() throws Exception {
        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Working on a fix.\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    void deleteUsesCommentIdRouteAndReturnsNoContent() throws Exception {
        var commentId = UUID.randomUUID();

        mockMvc.perform(delete("/api/comments/{commentId}", commentId))
                .andExpect(status().isNoContent());

        verify(commentService).delete(commentId);
    }

    private static CommentResponse commentResponse(UUID commentId, UUID reportId) {
        return new CommentResponse(
                commentId,
                reportId,
                UUID.randomUUID(),
                "Ramy",
                "Working on a fix.",
                LocalDateTime.now());
    }

    private static void authenticate(UUID accountId) {
        var account = org.mockito.Mockito.mock(com.ramy.bugreport.security.UserAccountDetails.class);
        when(account.getId()).thenReturn(accountId);
        var authentication = new UsernamePasswordAuthenticationToken(account, null, account.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
