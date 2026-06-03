package com.hakku.main.community.exception;

/**
 * 작성자가 아닌 회원이 게시글을 수정/삭제하려 할 때 발생. HTTP 403 으로 매핑된다.
 */
public class PostAccessDeniedException extends RuntimeException {

    public PostAccessDeniedException(Long postId) {
        super("게시글에 대한 권한이 없습니다: id=" + postId);
    }
}
