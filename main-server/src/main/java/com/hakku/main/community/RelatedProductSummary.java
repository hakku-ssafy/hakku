package com.hakku.main.community;

/**
 * 게시글에 연결된 연관 상품 요약 (학생증 자랑 → 상품 페이지 이동용).
 * 게시물에서 바로 {@code /products/{id}} 로 이동할 수 있도록 최소 정보만 담는다.
 */
public record RelatedProductSummary(Long id, String name, String imageUrl, long price) {
}
