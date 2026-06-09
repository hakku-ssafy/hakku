package com.hakku.main.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hakku.main.community.domain.Post;
import com.hakku.main.community.exception.PostAccessDeniedException;
import com.hakku.main.community.exception.PostNotFoundException;
import com.hakku.main.community.repository.CommentRepository;
import com.hakku.main.community.repository.PostLikeRepository;
import com.hakku.main.community.repository.PostRepository;
import com.hakku.main.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    private static final Long AUTHOR = 1L;
    private static final Long OTHER = 2L;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, postLikeRepository, commentRepository, userRepository);
    }

    @Test
    @DisplayName("create: 게시글을 저장하고 응답을 반환한다")
    void create_savesPost() {
        when(postRepository.save(any(Post.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PostResponse response = postService.create(AUTHOR, "제목", "본문");

        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.content()).isEqualTo("본문");
        assertThat(response.authorId()).isEqualTo(AUTHOR);
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("get: 없는 게시글이면 PostNotFoundException")
    void get_missing_throws() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.get(99L, null))
                .isInstanceOf(PostNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("update: 작성자 본인이면 제목/본문을 갱신한다")
    void update_byAuthor_updates() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post()));

        PostResponse response = postService.update(1L, AUTHOR, "새제목", "새본문");

        assertThat(response.title()).isEqualTo("새제목");
        assertThat(response.content()).isEqualTo("새본문");
    }

    @Test
    @DisplayName("update: 작성자가 아니면 PostAccessDeniedException")
    void update_byOther_throws() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post()));

        assertThatThrownBy(() -> postService.update(1L, OTHER, "x", "y"))
                .isInstanceOf(PostAccessDeniedException.class);
    }

    @Test
    @DisplayName("delete: 작성자 본인이면 삭제한다")
    void delete_byAuthor_deletes() {
        Post post = post();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        postService.delete(1L, AUTHOR);

        verify(postRepository).delete(post);
    }

    @Test
    @DisplayName("delete: 작성자가 아니면 거부하고 삭제하지 않는다")
    void delete_byOther_throws() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post()));

        assertThatThrownBy(() -> postService.delete(1L, OTHER))
                .isInstanceOf(PostAccessDeniedException.class);
        verify(postRepository, never()).delete(any(Post.class));
    }

    @Test
    @DisplayName("list: 모든 게시글을 응답으로 변환한다")
    void list_returnsAll() {
        when(postRepository.findAllByOrderByIdDesc()).thenReturn(List.of(post(), post()));

        List<PostResponse> all = postService.list((Long) null);

        assertThat(all).hasSize(2);
    }

    private Post post() {
        return new Post(AUTHOR, "제목", "본문");
    }
}
