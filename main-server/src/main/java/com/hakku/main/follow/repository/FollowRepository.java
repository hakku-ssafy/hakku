package com.hakku.main.follow.repository;

import com.hakku.main.follow.domain.Follow;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

    /** 이 회원을 팔로우하는 사람 수(팔로워). */
    long countByFollowingId(Long followingId);

    /** 이 회원이 팔로우하는 사람 수(팔로잉). */
    long countByFollowerId(Long followerId);

    /** 이 회원을 팔로우한 관계를 최신순으로 조회한다(팔로워 목록). */
    List<Follow> findByFollowingIdOrderByIdDesc(Long followingId);

    /** 이 회원이 팔로우한 관계를 최신순으로 조회한다(팔로잉 목록). */
    List<Follow> findByFollowerIdOrderByIdDesc(Long followerId);
}
