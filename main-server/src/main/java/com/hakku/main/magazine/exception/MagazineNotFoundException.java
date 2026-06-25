package com.hakku.main.magazine.exception;

/**
 * 요청한 매거진이 존재하지 않을 때 발생. HTTP 404 로 매핑된다.
 */
public class MagazineNotFoundException extends RuntimeException {

    public MagazineNotFoundException(Long id) {
        super("매거진을 찾을 수 없습니다: id=" + id);
    }
}
