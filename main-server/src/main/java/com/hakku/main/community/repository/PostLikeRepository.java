package com.hakku.main.community.repository;

import com.hakku.main.community.domain.PostLike;
import java.time.Instant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 게시글 좋아요 MyBatis 매퍼. SQL 은 {@code mapper/community/PostLikeMapper.xml} 에 정의한다.
 */
@Mapper
public interface PostLikeRepository {

    boolean existsByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    void deleteByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    long countByPostId(@Param("postId") Long postId);

    void insert(PostLike postLike);

    /** 좋아요를 저장한다. createdAt 만 있는 엔티티라 항상 신규 insert 다(JPA save() 의미 보존). */
    default PostLike save(PostLike postLike) {
        postLike.assignCreationTime(Instant.now());
        insert(postLike);
        return postLike;
    }
}
