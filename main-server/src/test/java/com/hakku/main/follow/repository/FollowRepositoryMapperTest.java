package com.hakku.main.follow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hakku.main.follow.domain.Follow;
import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * FollowRepository(MyBatis 매퍼) 통합 테스트. 실제 SQL 실행을 검증한다.
 * H2 스키마는 Flyway 마이그레이션(실제 운영 스키마)으로 제공되며 FK 제약을 강제한다.
 * 따라서 팔로우를 넣기 전에 참조 부모 행(users)을 먼저 생성하고 생성된 실제 id 를 사용한다.
 */
@SpringBootTest
@Transactional
class FollowRepositoryMapperTest {

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private UserRepository userRepository;

    private Long followerId;
    private Long followingId;
    private Long follower2Id;

    @BeforeEach
    void setUp() {
        followerId = userRepository.save(new User("follow-follower1@test.com", "팔로워1", Role.NORMAL)).getId();
        followingId = userRepository.save(new User("follow-following@test.com", "팔로잉대상", Role.NORMAL)).getId();
        follower2Id = userRepository.save(new User("follow-follower2@test.com", "팔로워2", Role.NORMAL)).getId();
    }

    @Test
    @DisplayName("save 후 존재/카운트가 반영된다")
    void save_thenExistsAndCount() {
        Follow saved = followRepository.save(new Follow(followerId, followingId));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)).isTrue();
        assertThat(followRepository.existsByFollowerIdAndFollowingId(followerId, 999L)).isFalse();
        assertThat(followRepository.countByFollowingId(followingId)).isEqualTo(1L);
        assertThat(followRepository.countByFollowerId(followerId)).isEqualTo(1L);
    }

    @Test
    @DisplayName("팔로워/팔로잉 목록을 id 내림차순으로 조회한다")
    void findLists_orderedByIdDesc() {
        followRepository.save(new Follow(followerId, followingId));
        followRepository.save(new Follow(follower2Id, followingId));

        List<Follow> followers = followRepository.findByFollowingIdOrderByIdDesc(followingId);
        assertThat(followers).extracting(Follow::getFollowerId).containsExactly(follower2Id, followerId);

        List<Follow> following = followRepository.findByFollowerIdOrderByIdDesc(followerId);
        assertThat(following).extracting(Follow::getFollowingId).containsExactly(followingId);
    }

    @Test
    @DisplayName("deleteByFollowerIdAndFollowingId 후 존재하지 않는다")
    void delete_thenAbsent() {
        followRepository.save(new Follow(followerId, followingId));

        followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);

        assertThat(followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)).isFalse();
        assertThat(followRepository.countByFollowingId(followingId)).isEqualTo(0L);
    }
}
