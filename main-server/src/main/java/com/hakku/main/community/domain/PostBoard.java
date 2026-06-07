package com.hakku.main.community.domain;

/**
 * 커뮤니티 게시판 종류.
 *
 * <ul>
 *   <li>{@link #GENERAL} — 일반 커뮤니티 글(텍스트 중심)</li>
 *   <li>{@link #STUDENT_ID} — '학생증 자랑' 게시판(꾸민 학생증 이미지 공유)</li>
 * </ul>
 */
public enum PostBoard {
    GENERAL,
    STUDENT_ID;

    /** 문자열을 게시판으로 변환한다. null/공백/미지원 값은 {@link #GENERAL} 로 처리한다. */
    public static PostBoard fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return GENERAL;
        }
        try {
            return PostBoard.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return GENERAL;
        }
    }
}
