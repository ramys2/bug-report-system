package com.ramy.bugreport.controller;

import static org.mockito.Mockito.verify;
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
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCommentsUsesReportCommentsRoute() throws Exception {
        var reportId = UUID.randomUUID();
        var commentId = UUID.randomUUID();
        when(commentService.getComments(reportId))
                .thenReturn(List.of(commentResponse(commentId, reportId)));

        mockMvc.perform(get("/api/reports/{reportId}/comments", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(commentId.toString()))
                .andExpect(jsonPath("$[0].bug_report_id").value(reportId.toString()))
                .andExpect(jsonPath("$[0].author_name").value("Ramy"));

        verify(commentService).getComments(reportId);
    }

    @Test
    void createUsesReportCommentsRouteAndReturnsCreated() throws Exception {
        var reportId = UUID.randomUUID();
        var authorId = UUID.randomUUID();
        var commentId = UUID.randomUUID();
        var createdAt = LocalDateTime.now();
        var request = new CreateCommentRequest("Working on a fix.");
        when(commentService.create(reportId, authorId, request))
                .thenReturn(new CreateCommentResponse(
                        commentId, authorId, "Ramy", request.content(), createdAt));

        authenticate(authorId);

        mockMvc.perform(post("/api/reports/{reportId}/comments", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Working on a fix."
                                }
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(commentId.toString()))
                .andExpect(jsonPath("$.author_id").value(authorId.toString()))
                .andExpect(jsonPath("$.author_name").value("Ramy"))
                .andExpect(jsonPath("$.content").value(request.content()))
                .andExpect(jsonPath("$.created_at").isNotEmpty());

        verify(commentService).create(reportId, authorId, request);
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
