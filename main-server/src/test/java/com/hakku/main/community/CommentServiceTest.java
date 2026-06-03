package com.hakku.main.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hakku.main.community.domain.Comment;
import com.hakku.main.community.exception.CommentAccessDeniedException;
import com.hakku.main.community.exception.CommentNotFoundException;
import com.hakku.main.community.exception.PostNotFoundException;
import com.hakku.main.community.repository.CommentRepository;
import com.hakku.main.community.repository.PostRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    private static final Long POST = 10L;
    private static final Long AUTHOR = 1L;
    private static final Long OTHER = 2L;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentRepository, postRepository);
    }

    @Test
    @DisplayName("create: 게시글이 존재하면 댓글을 저장한다")
    void create_savesComment() {
        when(postRepository.existsById(POST)).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        CommentResponse response = commentService.create(POST, AUTHOR, "좋은 글이네요");

        assertThat(response.content()).isEqualTo("좋은 글이네요");
        assertThat(response.postId()).isEqualTo(POST);
        assertThat(response.authorId()).isEqualTo(AUTHOR);
    }

    @Test
    @DisplayName("create: 게시글이 없으면 PostNotFoundException, 저장하지 않는다")
    void create_missingPost_throws() {
        when(postRepository.existsById(POST)).thenReturn(false);

        assertThatThrownBy(() -> commentService.create(POST, AUTHOR, "x"))
                .isInstanceOf(PostNotFoundException.class);
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("listByPost: 게시글의 댓글을 응답으로 변환한다")
    void listByPost_returnsAll() {
        when(commentRepository.findByPostIdOrderByIdAsc(POST))
                .thenReturn(List.of(comment(), comment()));

        List<CommentResponse> all = commentService.listByPost(POST);

        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("update: 작성자 본인이면 내용을 갱신한다")
    void update_byAuthor_updates() {
        when(commentRepository.findById(5L)).thenReturn(Optional.of(comment()));

        CommentResponse response = commentService.update(5L, AUTHOR, "수정된 댓글");

        assertThat(response.content()).isEqualTo("수정된 댓글");
    }

    @Test
    @DisplayName("update: 작성자가 아니면 CommentAccessDeniedException")
    void update_byOther_throws() {
        when(commentRepository.findById(5L)).thenReturn(Optional.of(comment()));

        assertThatThrownBy(() -> commentService.update(5L, OTHER, "x"))
                .isInstanceOf(CommentAccessDeniedException.class);
    }

    @Test
    @DisplayName("update: 없는 댓글이면 CommentNotFoundException")
    void update_missing_throws() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.update(99L, AUTHOR, "x"))
                .isInstanceOf(CommentNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("delete: 작성자가 아니면 거부하고 삭제하지 않는다")
    void delete_byOther_throws() {
        when(commentRepository.findById(5L)).thenReturn(Optional.of(comment()));

        assertThatThrownBy(() -> commentService.delete(5L, OTHER))
                .isInstanceOf(CommentAccessDeniedException.class);
        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    @DisplayName("delete: 작성자 본인이면 삭제한다")
    void delete_byAuthor_deletes() {
        Comment comment = comment();
        when(commentRepository.findById(5L)).thenReturn(Optional.of(comment));

        commentService.delete(5L, AUTHOR);

        verify(commentRepository).delete(comment);
    }

    private Comment comment() {
        return new Comment(POST, AUTHOR, "본문 댓글");
    }
}
