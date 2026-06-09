package com.hakku.main.user;

import com.hakku.main.personalcolor.domain.PersonalColorType;
import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import java.util.Set;

/**
 * 공개 프로필 응답 DTO (요구사항 §1). 다른 회원이 볼 수 있는 정보 + 팔로우 통계.
 */
public record PublicProfileResponse(
        Long id,
        String nickname,
        String profileImageUrl,
        Role role,
        PersonalColorType personalColor,
        Set<String> preferredColors,
        Set<String> preferredStyles,
        long followerCount,
        long followingCount,
        boolean followedByMe) {

    public static PublicProfileResponse from(User user, long followerCount, long followingCount,
                                             boolean followedByMe) {
        return new PublicProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getRole(),
                user.getPersonalColor(),
                Set.copyOf(user.getPreferredColors()),
                Set.copyOf(user.getPreferredStyles()),
                followerCount,
                followingCount,
                followedByMe);
    }
}
