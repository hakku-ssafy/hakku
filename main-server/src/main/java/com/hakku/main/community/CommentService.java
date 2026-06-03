package com.hakku.main.community;

import com.hakku.main.community.domain.Comment;
import com.hakku.main.community.exception.CommentAccessDeniedException;
import com.hakku.main.community.exception.CommentNotFoundException;
import com.hakku.main.community.exception.PostNotFoundException;
import com.hakku.main.community.repository.CommentRepository;
import com.hakku.main.community.repository.PostRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 댓글 CRUD. 작성 시 대상 게시글 존재를 검증하고, 수정/삭제는 작성자 본인만 가능하다 (PRD §3.4).
 */
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }

    @Transactional
    public CommentResponse create(Long postId, Long authorId, String content) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException(postId);
        }
        Comment saved = commentRepository.save(new Comment(postId, authorId, content));
        return CommentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listByPost(Long postId) {
        return commentRepository.findByPostIdOrderByIdAsc(postId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public CommentResponse update(Long commentId, Long requesterId, String content) {
        Comment comment = findOrThrow(commentId);
        requireAuthor(comment, requesterId);
        comment.update(content);
        return CommentResponse.from(comment);
    }

    @Transactional
    public void delete(Long commentId, Long requesterId) {
        Comment comment = findOrThrow(commentId);
        requireAuthor(comment, requesterId);
        commentRepository.delete(comment);
    }

    private Comment findOrThrow(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
    }

    private void requireAuthor(Comment comment, Long requesterId) {
        if (!comment.isAuthor(requesterId)) {
            throw new CommentAccessDeniedException(comment.getId());
        }
    }
}
