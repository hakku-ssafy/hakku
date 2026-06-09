package com.hakku.main.follow.exception;

/** 자기 자신을 팔로우하려 할 때. */
public class SelfFollowException extends RuntimeException {

    public SelfFollowException() {
        super("자기 자신은 팔로우할 수 없습니다.");
    }
}
