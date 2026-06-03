package com.hakku.main.community;

import com.hakku.main.community.domain.Comment;
import java.time.Instant;

/**
 * 댓글 응답 DTO.
 */
public record CommentResponse(
        Long id,
        Long postId,
        Long authorId,
        String content,
        Instant createdAt,
        Instant updatedAt) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getAuthorId(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}
