package com.hakku.main.product;

import java.util.Set;

/**
 * 상품 생성/수정 입력. 컨트롤러 요청과 서비스 사이의 명령 객체.
 */
public record ProductCommand(
        String name,
        String description,
        long price,
        String keyColor,
        String subColor,
        Set<String> styles,
        String imageUrl) {
}
