package com.hakku.main.community;

import com.hakku.main.community.domain.Post;
import com.hakku.main.community.domain.PostLike;
import com.hakku.main.community.exception.PostNotFoundException;
import com.hakku.main.community.repository.PostLikeRepository;
import com.hakku.main.community.repository.PostRepository;
import com.hakku.main.notification.NotificationEvent;
import com.hakku.main.notification.NotificationProducer;
import com.hakku.main.notification.domain.NotificationType;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 좋아요 토글 (PRD §3.4). 누르지 않았으면 추가, 이미 눌렀으면 취소한다.
 */
@Service
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final NotificationProducer notificationProducer;

    public PostLikeService(PostLikeRepository postLikeRepository, PostRepository postRepository,
                           NotificationProducer notificationProducer) {
        this.postLikeRepository = postLikeRepository;
        this.postRepository = postRepository;
        this.notificationProducer = notificationProducer;
    }

    @Transactional
    public LikeResult toggle(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
        boolean liked;
        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            postLikeRepository.deleteByPostIdAndUserId(postId, userId);
            liked = false;
        } else {
            postLikeRepository.save(new PostLike(postId, userId));
            liked = true;
            if (!post.getAuthorId().equals(userId)) {
                notificationProducer.publish(new NotificationEvent(
                        NotificationType.LIKE, post.getAuthorId(), userId,
                        "좋아요를 눌렀습니다.", Instant.now().toEpochMilli()));
            }
        }
        return new LikeResult(liked, postLikeRepository.countByPostId(postId));
    }
}
