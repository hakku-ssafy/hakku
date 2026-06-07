package com.hakku.main.community;

import com.hakku.main.community.domain.Post;
import java.time.Instant;

/**
 * 게시글 응답 DTO.
 */
public record PostResponse(
        Long id,
        Long authorId,
        String authorNickname,
        String title,
        String content,
        String board,
        String imageUrl,
        long likeCount,
        long commentCount,
        boolean liked,
        Instant createdAt,
        Instant updatedAt) {

    public static PostResponse from(Post post, String authorNickname, long likeCount,
                                  long commentCount, boolean liked) {
        return new PostResponse(
                post.getId(),
                post.getAuthorId(),
                authorNickname,
                post.getTitle(),
                post.getContent(),
                post.getBoard().name(),
                post.getImageUrl(),
                likeCount,
                commentCount,
                liked,
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
