package com.ramy.bugreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ramy.bugreport.domain.Comment;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.dto.comment.CreateCommentRequest;
import com.ramy.bugreport.exception.ResourceNotFoundException;
import com.ramy.bugreport.repository.IBugReportRepository;
import com.ramy.bugreport.repository.ICommentRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private ICommentRepository commentRepository;
    @Mock
    private IBugReportRepository bugReportRepository;
    @Mock
    private IUserAccountRepository userAccountRepository;

    private CommentService service;

    @BeforeEach
    void setUp() {
        service = new CommentService(commentRepository, bugReportRepository, userAccountRepository);
    }

    @Test
    void getCommentsMapsCommentsForExistingReport() {
        var reportId = UUID.randomUUID();
        var comment = comment(reportId);
        when(bugReportRepository.existsById(reportId)).thenReturn(true);
        when(commentRepository.findByBugReportId(reportId)).thenReturn(List.of(comment));

        var result = service.getComments(reportId);

        assertThat(result)
                .extracting(response -> response.id())
                .containsExactly(comment.getId());
        verify(commentRepository).findByBugReportId(reportId);
    }

    @Test
    void getCommentsThrowsWhenReportDoesNotExist() {
        var reportId = UUID.randomUUID();
        when(bugReportRepository.existsById(reportId)).thenReturn(false);

        assertThatThrownBy(() -> service.getComments(reportId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Report with id: %s".formatted(reportId));
        verifyNoInteractions(commentRepository);
    }

    @Test
    void createValidatesReferencesBuildsAndSavesComment() {
        var reportId = UUID.randomUUID();
        var authorId = UUID.randomUUID();
        var request = new CreateCommentRequest("Working on a fix.");
        var commentId = UUID.randomUUID();
        var savedComment = org.mockito.Mockito.mock(Comment.class);
        var author = org.mockito.Mockito.mock(UserAccount.class);
        when(bugReportRepository.existsById(reportId)).thenReturn(true);
        when(userAccountRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
        when(savedComment.getId()).thenReturn(commentId);
        when(savedComment.getAuthorId()).thenReturn(authorId);
        when(savedComment.getContent()).thenReturn(request.content());
        when(savedComment.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(author.getName()).thenReturn("Ramy");
        var before = LocalDateTime.now();

        var result = service.create(reportId, authorId, request);

        var commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(commentCaptor.capture());
        var saved = commentCaptor.getValue();
        assertThat(saved.getBugReportId()).isEqualTo(reportId);
        assertThat(saved.getAuthorId()).isEqualTo(authorId);
        assertThat(saved.getContent()).isEqualTo(request.content());
        assertThat(saved.getCreatedAt()).isBetween(before, LocalDateTime.now());
        assertThat(result.id()).isEqualTo(commentId);
        assertThat(result.authorId()).isEqualTo(authorId);
        assertThat(result.authorName()).isEqualTo("Ramy");
        assertThat(result.content()).isEqualTo(request.content());
        assertThat(result.createdAt()).isNotNull();
    }

    @Test
    void createThrowsWhenReportDoesNotExist() {
        var reportId = UUID.randomUUID();
        var authorId = UUID.randomUUID();
        var request = new CreateCommentRequest("Working on a fix.");
        when(bugReportRepository.existsById(reportId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(reportId, authorId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Report with id: %s".formatted(reportId));
        verifyNoInteractions(userAccountRepository, commentRepository);
    }

    @Test
    void createThrowsWhenAuthorDoesNotExist() {
        var reportId = UUID.randomUUID();
        var authorId = UUID.randomUUID();
        var request = new CreateCommentRequest("Working on a fix.");
        when(bugReportRepository.existsById(reportId)).thenReturn(true);
        when(userAccountRepository.findById(authorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(reportId, authorId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User with id=%s does not exist!".formatted(authorId));
        verifyNoInteractions(commentRepository);
    }

    @Test
    void deleteDeletesExistingComment() {
        var commentId = UUID.randomUUID();
        var comment = comment(UUID.randomUUID());
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        service.delete(commentId);

        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteThrowsWhenCommentDoesNotExist() {
        var commentId = UUID.randomUUID();
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(commentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Comment with id: %s".formatted(commentId));
    }

    private static Comment comment(UUID reportId) {
        return new Comment(
                reportId,
                UUID.randomUUID(),
                "Working on a fix.",
                LocalDateTime.now());
    }
}
