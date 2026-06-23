package com.hakku.main.follow.repository;

import com.hakku.main.follow.domain.Follow;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 팔로우 MyBatis 매퍼. SQL 은 {@code mapper/follow/FollowMapper.xml} 에 정의한다.
 */
@Mapper
public interface FollowRepository {

    boolean existsByFollowerIdAndFollowingId(@Param("followerId") Long followerId,
                                             @Param("followingId") Long followingId);

    void deleteByFollowerIdAndFollowingId(@Param("followerId") Long followerId,
                                          @Param("followingId") Long followingId);

    /** 이 회원을 팔로우하는 사람 수(팔로워). */
    long countByFollowingId(@Param("followingId") Long followingId);

    /** 이 회원이 팔로우하는 사람 수(팔로잉). */
    long countByFollowerId(@Param("followerId") Long followerId);

    /** 이 회원을 팔로우한 관계를 최신순으로 조회한다(팔로워 목록). */
    List<Follow> findByFollowingIdOrderByIdDesc(@Param("followingId") Long followingId);

    /** 이 회원이 팔로우한 관계를 최신순으로 조회한다(팔로잉 목록). */
    List<Follow> findByFollowerIdOrderByIdDesc(@Param("followerId") Long followerId);

    void insert(Follow follow);

    /** 팔로우를 저장한다. createdAt 만 있는 엔티티라 항상 신규 insert 다(JPA save() 의미 보존). */
    default Follow save(Follow follow) {
        follow.assignCreationTime(Instant.now());
        insert(follow);
        return follow;
    }
}
