package com.hakku.main.community;

import com.hakku.main.community.domain.Post;
import java.time.Instant;

/**
 * 게시글 응답 DTO.
 */
public record PostResponse(
        Long id,
        Long authorId,
        String title,
        String content,
        Instant createdAt,
        Instant updatedAt) {

    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getAuthorId(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
