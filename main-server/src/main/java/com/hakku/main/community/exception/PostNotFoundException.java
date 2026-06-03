package com.hakku.main.community.exception;

/**
 * 요청한 게시글이 존재하지 않을 때 발생. HTTP 404 로 매핑된다.
 */
public class PostNotFoundException extends RuntimeException {

    public PostNotFoundException(Long postId) {
        super("게시글을 찾을 수 없습니다: id=" + postId);
    }
}
