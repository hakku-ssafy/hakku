package com.hakku.main.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.hakku.main.community.CommentService;
import com.hakku.main.community.PostLikeService;
import com.hakku.main.community.domain.Post;
import com.hakku.main.community.repository.PostLikeRepository;
import com.hakku.main.community.repository.PostRepository;
import com.hakku.main.community.repository.CommentRepository;
import com.hakku.main.notification.domain.NotificationType;
import com.hakku.main.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationPublishingTest {

    // ── Comment ──────────────────────────────────────────────────────────────────

    @Mock CommentRepository commentRepository;
    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationProducer notificationProducer;
    @InjectMocks CommentService commentService;

    @Test
    @DisplayName("댓글 작성 시 게시글 작성자에게 COMMENT 알림 발행")
    void createComment_publishesCommentNotification() {
        Post post = makePost(1L, 99L);
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(commentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        commentService.create(1L, 2L, "좋은 글이네요");

        verify(notificationProducer).publish(argThat(event ->
                event.type() == NotificationType.COMMENT
                && event.recipientId().equals(99L)
                && event.actorId().equals(2L)));
    }

    @Test
    @DisplayName("자기 게시글에 댓글 작성 시 알림 미발행")
    void createComment_noNotificationForSelfComment() {
        Post post = makePost(1L, 2L);
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(commentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        commentService.create(1L, 2L, "내 글에 댓글");

        verify(notificationProducer, never()).publish(any());
    }

    // ── PostLike ─────────────────────────────────────────────────────────────────

    @Mock PostLikeRepository postLikeRepository;
    @InjectMocks PostLikeService postLikeService;

    @Test
    @DisplayName("좋아요 추가 시 게시글 작성자에게 LIKE 알림 발행")
    void toggleLike_publishesLikeNotificationWhenLiked() {
        Post post = makePost(1L, 99L);
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(postLikeRepository.existsByPostIdAndUserId(1L, 2L)).willReturn(false);
        given(postLikeRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(postLikeRepository.countByPostId(1L)).willReturn(1L);

        postLikeService.toggle(1L, 2L);

        verify(notificationProducer).publish(argThat(event ->
                event.type() == NotificationType.LIKE
                && event.recipientId().equals(99L)
                && event.actorId().equals(2L)));
    }

    @Test
    @DisplayName("좋아요 취소 시 알림 미발행")
    void toggleLike_noNotificationWhenUnliked() {
        Post post = makePost(1L, 99L);
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(postLikeRepository.existsByPostIdAndUserId(1L, 2L)).willReturn(true);
        given(postLikeRepository.countByPostId(1L)).willReturn(0L);

        postLikeService.toggle(1L, 2L);

        verify(notificationProducer, never()).publish(any());
    }

    @Test
    @DisplayName("자기 게시글 좋아요 시 알림 미발행")
    void toggleLike_noNotificationForSelfLike() {
        Post post = makePost(1L, 2L);
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(postLikeRepository.existsByPostIdAndUserId(1L, 2L)).willReturn(false);
        given(postLikeRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(postLikeRepository.countByPostId(1L)).willReturn(1L);

        postLikeService.toggle(1L, 2L);

        verify(notificationProducer, never()).publish(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────────

    private Post makePost(Long postId, Long authorId) {
        Post post = new Post(authorId, "제목", "내용");
        try {
            var field = Post.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(post, postId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return post;
    }
}
