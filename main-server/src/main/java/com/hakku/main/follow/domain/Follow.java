package com.hakku.main.follow.domain;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 간 팔로우. follower 가 following 을 팔로우한다. 쌍당 한 번만(유니크).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow {

    private Long id;

    private Long followerId;

    private Long followingId;

    private Instant createdAt;

    public Follow(Long followerId, Long followingId) {
        this.followerId = followerId;
        this.followingId = followingId;
    }

    /** 영속 시각을 부여한다(MyBatis insert 직전 호출). JPA @CreationTimestamp 대체. */
    public void assignCreationTime(Instant now) {
        this.createdAt = now;
    }
}
