package com.hakku.main.follow;

import com.hakku.main.follow.domain.Follow;
import com.hakku.main.follow.exception.SelfFollowException;
import com.hakku.main.follow.repository.FollowRepository;
import com.hakku.main.notification.NotificationEvent;
import com.hakku.main.notification.NotificationMessageFormatter;
import com.hakku.main.notification.NotificationProducer;
import com.hakku.main.notification.domain.NotificationType;
import com.hakku.main.user.UserSummaryResponse;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.exception.UserNotFoundException;
import com.hakku.main.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 간 팔로우 (요구사항 §1). 토글 방식으로 팔로우/언팔로우하며, 새로 팔로우하면 상대에게 알림이 간다.
 */
@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationProducer notificationProducer;

    public FollowService(FollowRepository followRepository, UserRepository userRepository,
                         NotificationProducer notificationProducer) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.notificationProducer = notificationProducer;
    }

    /** 팔로우를 토글한다. 누르지 않았으면 팔로우(+알림), 이미 팔로우 중이면 취소. */
    @Transactional
    public FollowToggleResponse toggle(Long followerId, Long targetId) {
        if (followerId.equals(targetId)) {
            throw new SelfFollowException();
        }
        if (!userRepository.existsById(targetId)) {
            throw new UserNotFoundException(targetId);
        }
        boolean following;
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, targetId)) {
            followRepository.deleteByFollowerIdAndFollowingId(followerId, targetId);
            following = false;
        } else {
            followRepository.save(new Follow(followerId, targetId));
            following = true;
            String actorNickname = userRepository.findById(followerId)
                    .map(User::getNickname)
                    .orElse("누군가");
            notificationProducer.publish(new NotificationEvent(
                    NotificationType.FOLLOW,
                    targetId,
                    followerId,
                    actorNickname,
                    null,
                    null,
                    null,
                    NotificationMessageFormatter.follow(actorNickname),
                    Instant.now().toEpochMilli()));
        }
        return new FollowToggleResponse(following, followRepository.countByFollowingId(targetId));
    }

    /** 이 회원을 팔로우하는 사람(팔로워) 목록을 최신순으로 조회한다. */
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listFollowers(Long userId) {
        List<Long> followerIds = followRepository.findByFollowingIdOrderByIdDesc(userId).stream()
                .map(Follow::getFollowerId)
                .toList();
        return toSummaries(followerIds);
    }

    /** 이 회원이 팔로우하는 사람(팔로잉) 목록을 최신순으로 조회한다. */
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listFollowing(Long userId) {
        List<Long> followingIds = followRepository.findByFollowerIdOrderByIdDesc(userId).stream()
                .map(Follow::getFollowingId)
                .toList();
        return toSummaries(followingIds);
    }

    /** id 순서를 유지하며 회원 요약으로 변환한다(삭제된 회원은 건너뛴다). */
    private List<UserSummaryResponse> toSummaries(List<Long> orderedIds) {
        if (orderedIds.isEmpty()) {
            return List.of();
        }
        Map<Long, User> byId = userRepository.findAllById(orderedIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return orderedIds.stream()
                .map(byId::get)
                .filter(user -> user != null)
                .map(UserSummaryResponse::from)
                .toList();
    }
}
