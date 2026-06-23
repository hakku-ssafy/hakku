package com.hakku.main.community;

import com.hakku.main.community.domain.Post;
import com.hakku.main.community.domain.PostBoard;
import com.hakku.main.community.exception.PostAccessDeniedException;
import com.hakku.main.community.exception.PostNotFoundException;
import com.hakku.main.community.repository.CommentRepository;
import com.hakku.main.community.repository.PostLikeRepository;
import com.hakku.main.community.repository.PostRepository;
import com.hakku.main.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 커뮤니티 게시글 CRUD. 수정/삭제는 작성자 본인만 가능하다 (PRD §3.4).
 */
@Service
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public PostService(PostRepository postRepository,
                       PostLikeRepository postLikeRepository,
                       CommentRepository commentRepository,
                       UserRepository userRepository) {
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PostResponse create(Long authorId, String title, String content) {
        return create(authorId, title, content, PostBoard.GENERAL, null);
    }

    @Transactional
    public PostResponse create(Long authorId, String title, String content,
                               PostBoard board, String imageUrl) {
        Post saved = postRepository.save(new Post(authorId, title, content, board, imageUrl));
        return toResponse(saved, authorId);
    }

    @Transactional(readOnly = true)
    public PostResponse get(Long postId, Long viewerId) {
        return toResponse(findOrThrow(postId), viewerId);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> list(Long viewerId) {
        return postRepository.findAllByOrderByIdDesc().stream()
                .map(post -> toResponse(post, viewerId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PostResponse> list(Long viewerId, PostBoard board) {
        if (board == null) {
            return list(viewerId);
        }
        return postRepository.findAllByBoardOrderByIdDesc(board).stream()
                .map(post -> toResponse(post, viewerId))
                .toList();
    }

    /** 특정 작성자가 쓴 게시글을 최신순으로 조회한다(마이페이지/프로필용). */
    @Transactional(readOnly = true)
    public List<PostResponse> listByAuthor(Long viewerId, Long authorId) {
        return postRepository.findAllByAuthorIdOrderByIdDesc(authorId).stream()
                .map(post -> toResponse(post, viewerId))
                .toList();
    }

    @Transactional
    public PostResponse update(Long postId, Long requesterId, String title, String content) {
        Post post = findOrThrow(postId);
        requireAuthor(post, requesterId);
        post.update(title, content);
        postRepository.save(post);
        return toResponse(post, requesterId);
    }

    @Transactional
    public void delete(Long postId, Long requesterId) {
        Post post = findOrThrow(postId);
        requireAuthor(post, requesterId);
        postRepository.delete(post);
    }

    private PostResponse toResponse(Post post, Long viewerId) {
        String authorNickname = userRepository.findById(post.getAuthorId())
                .map(user -> user.getNickname())
                .orElse("알 수 없음");
        long likeCount = postLikeRepository.countByPostId(post.getId());
        long commentCount = commentRepository.countByPostId(post.getId());
        boolean liked = viewerId != null
                && postLikeRepository.existsByPostIdAndUserId(post.getId(), viewerId);
        return PostResponse.from(post, authorNickname, likeCount, commentCount, liked);
    }

    private Post findOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
    }

    private void requireAuthor(Post post, Long requesterId) {
        if (!post.isAuthor(requesterId)) {
            throw new PostAccessDeniedException(post.getId());
        }
    }
}
