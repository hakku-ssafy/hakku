package com.hakku.main.follow;

/** 팔로우 토글 결과 DTO. */
public record FollowToggleResponse(boolean following, long followerCount) {
}
