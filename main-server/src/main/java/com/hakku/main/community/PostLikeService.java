package com.hakku.main.community;

import com.hakku.main.community.domain.PostLike;
import com.hakku.main.community.exception.PostNotFoundException;
import com.hakku.main.community.repository.PostLikeRepository;
import com.hakku.main.community.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 좋아요 토글 (PRD §3.4). 누르지 않았으면 추가, 이미 눌렀으면 취소한다.
 */
@Service
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;

    public PostLikeService(PostLikeRepository postLikeRepository, PostRepository postRepository) {
        this.postLikeRepository = postLikeRepository;
        this.postRepository = postRepository;
    }

    @Transactional
    public LikeResult toggle(Long postId, Long userId) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException(postId);
        }
        boolean liked;
        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            postLikeRepository.deleteByPostIdAndUserId(postId, userId);
            liked = false;
        } else {
            postLikeRepository.save(new PostLike(postId, userId));
            liked = true;
        }
        return new LikeResult(liked, postLikeRepository.countByPostId(postId));
    }
}
