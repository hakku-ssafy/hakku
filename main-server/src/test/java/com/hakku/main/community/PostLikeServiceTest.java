package com.hakku.main.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hakku.main.community.domain.PostLike;
import com.hakku.main.community.exception.PostNotFoundException;
import com.hakku.main.community.repository.PostLikeRepository;
import com.hakku.main.community.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    private static final Long POST = 10L;
    private static final Long USER = 1L;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostRepository postRepository;

    private PostLikeService postLikeService;

    @BeforeEach
    void setUp() {
        postLikeService = new PostLikeService(postLikeRepository, postRepository);
    }

    @Test
    @DisplayName("toggle: 처음 누르면 좋아요를 추가하고 liked=true")
    void toggle_firstTime_likes() {
        when(postRepository.existsById(POST)).thenReturn(true);
        when(postLikeRepository.existsByPostIdAndUserId(POST, USER)).thenReturn(false);
        when(postLikeRepository.countByPostId(POST)).thenReturn(1L);

        LikeResult result = postLikeService.toggle(POST, USER);

        assertThat(result.liked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(1L);
        verify(postLikeRepository).save(any(PostLike.class));
    }

    @Test
    @DisplayName("toggle: 이미 눌렀으면 좋아요를 취소하고 liked=false")
    void toggle_alreadyLiked_unlikes() {
        when(postRepository.existsById(POST)).thenReturn(true);
        when(postLikeRepository.existsByPostIdAndUserId(POST, USER)).thenReturn(true);
        when(postLikeRepository.countByPostId(POST)).thenReturn(0L);

        LikeResult result = postLikeService.toggle(POST, USER);

        assertThat(result.liked()).isFalse();
        assertThat(result.likeCount()).isEqualTo(0L);
        verify(postLikeRepository).deleteByPostIdAndUserId(POST, USER);
        verify(postLikeRepository, never()).save(any(PostLike.class));
    }

    @Test
    @DisplayName("toggle: 없는 게시글이면 PostNotFoundException")
    void toggle_missingPost_throws() {
        when(postRepository.existsById(POST)).thenReturn(false);

        assertThatThrownBy(() -> postLikeService.toggle(POST, USER))
                .isInstanceOf(PostNotFoundException.class);
        verify(postLikeRepository, never()).save(any(PostLike.class));
    }
}
