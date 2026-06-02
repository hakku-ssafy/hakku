package com.hakku.main.user.domain;

/**
 * 회원 역할 (PRD §2).
 *
 * <ul>
 *   <li>{@link #NORMAL} — 일반 회원: 조회/장바구니/리뷰/커뮤니티/AI 진단</li>
 *   <li>{@link #SELLER} — 판매자: 일반 회원 기능 + 상품 등록/수정/삭제</li>
 *   <li>{@link #ADMIN}  — 관리자 (추후 확장)</li>
 * </ul>
 */
public enum Role {
    NORMAL,
    SELLER,
    ADMIN
}
