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
                .build();
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
                .andExpect(jsonPath("$[0].bugReportId").value(reportId.toString()));

        verify(commentService).getComments(reportId);
    }

    @Test
    void createUsesReportCommentsRouteAndReturnsCreated() throws Exception {
        var reportId = UUID.randomUUID();
        var authorId = UUID.randomUUID();
        var commentId = UUID.randomUUID();
        var createdAt = LocalDateTime.now();
        var request = new CreateCommentRequest(authorId, "Working on a fix.");
        when(commentService.create(reportId, request))
                .thenReturn(new CreateCommentResponse(
                        commentId, authorId, "Ramy", request.content(), createdAt));

        mockMvc.perform(post("/api/reports/{reportId}/comments", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authorId": "%s",
                                  "content": "Working on a fix."
                                }
                """.formatted(authorId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(commentId.toString()))
                .andExpect(jsonPath("$.authorId").value(authorId.toString()))
                .andExpect(jsonPath("$.authorName").value("Ramy"))
                .andExpect(jsonPath("$.content").value(request.content()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        verify(commentService).create(reportId, request);
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
                "Working on a fix.",
                LocalDateTime.now());
    }
}
