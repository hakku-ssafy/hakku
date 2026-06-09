package com.hakku.main.user;

import com.hakku.main.personalcolor.domain.PersonalColorType;
import com.hakku.main.user.domain.User;

/** 목록(팔로워/팔로잉 등)에 쓰이는 회원 요약 DTO. */
public record UserSummaryResponse(
        Long id,
        String nickname,
        String profileImageUrl,
        PersonalColorType personalColor) {

    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getPersonalColor());
    }
}
