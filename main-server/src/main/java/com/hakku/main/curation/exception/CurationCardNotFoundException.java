package com.hakku.main.curation.exception;

/**
 * 요청한 큐레이션 카드가 존재하지 않을 때 발생. HTTP 404 로 매핑된다.
 */
public class CurationCardNotFoundException extends RuntimeException {

    public CurationCardNotFoundException(Long id) {
        super("큐레이션 카드를 찾을 수 없습니다: id=" + id);
    }
}
