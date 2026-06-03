package com.hakku.main.community.exception;

/**
 * 요청한 댓글이 존재하지 않을 때 발생. HTTP 404 로 매핑된다.
 */
public class CommentNotFoundException extends RuntimeException {

    public CommentNotFoundException(Long commentId) {
        super("댓글을 찾을 수 없습니다: id=" + commentId);
    }
}
