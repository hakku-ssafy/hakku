package com.hakku.main.community;

import com.hakku.main.community.domain.Post;
import java.time.Instant;
import java.util.List;

/**
 * 게시글 응답 DTO. 연관 상품(relatedProducts)은 '학생증 자랑' 글에서 상품 페이지로 바로 이동하는 데 쓰인다.
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
        List<RelatedProductSummary> relatedProducts,
        Instant createdAt,
        Instant updatedAt) {

    public static PostResponse from(Post post, String authorNickname, long likeCount,
                                  long commentCount, boolean liked,
                                  List<RelatedProductSummary> relatedProducts) {
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
                relatedProducts == null ? List.of() : List.copyOf(relatedProducts),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
