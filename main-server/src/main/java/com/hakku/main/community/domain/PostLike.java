package com.hakku.main.community.domain;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 좋아요 (PRD §3.4). 회원-게시글 당 한 번만 (유니크).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike {

    private Long id;

    private Long postId;

    private Long userId;

    private Instant createdAt;

    public PostLike(Long postId, Long userId) {
        this.postId = postId;
        this.userId = userId;
    }

    /** 영속 시각을 부여한다(MyBatis insert 직전 호출). JPA @CreationTimestamp 대체. */
    public void assignCreationTime(Instant now) {
        this.createdAt = now;
    }
}
