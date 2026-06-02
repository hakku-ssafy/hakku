package com.hakku.main.personalcolor.domain;

import java.util.Locale;
import java.util.Optional;

/**
 * 16종 퍼스널컬러 분류 (사용자 확정 명칭). 톤은 계절마다 다르다.
 *
 * <p>AI 진단 결과 이미지를 OCR로 읽어 추출한 텍스트를 이 enum 값으로 매핑한다
 * ({@link #fromText(String)}). 매칭은 <b>계절 범위 내(season-scoped)</b>로 수행한다:
 * 먼저 계절을 식별하고, 그 계절에 속한 타입들 사이에서만 톤 키워드를 매칭한다.
 * 'cool' 처럼 계절마다 의미가 다른 단어의 전역 모호성을 피하기 위함이다.
 *
 * <p>각 톤 키워드는 한글/영문(별칭 포함)을 담는다. 한글 substring 충돌
 * (예: "브라이트" ⊃ "라이트")은 가장 긴 키워드 매칭을 우선해 해소한다.
 */
public enum PersonalColorType {

    // ── 봄 (Spring) ──────────────────────────────
    LIGHT_SPRING(Season.SPRING, "light", "라이트"),
    TRUE_SPRING(Season.SPRING, "true", "warm", "트루", "웜"),
    BRIGHT_SPRING(Season.SPRING, "bright", "브라이트"),
    CLEAR_SPRING(Season.SPRING, "clear", "클리어"),

    // ── 여름 (Summer) ────────────────────────────
    LIGHT_SUMMER(Season.SUMMER, "light", "라이트"),
    TRUE_SUMMER(Season.SUMMER, "true", "트루"),
    SOFT_SUMMER(Season.SUMMER, "soft", "소프트"),
    COOL_SUMMER(Season.SUMMER, "cool", "쿨"),

    // ── 가을 (Autumn) ────────────────────────────
    SOFT_AUTUMN(Season.AUTUMN, "soft", "소프트"),
    TRUE_AUTUMN(Season.AUTUMN, "true", "warm", "트루", "웜"),
    DEEP_AUTUMN(Season.AUTUMN, "deep", "딥"),
    MUTED_AUTUMN(Season.AUTUMN, "muted", "mute", "뮤트"),

    // ── 겨울 (Winter) ────────────────────────────
    BRIGHT_WINTER(Season.WINTER, "bright", "브라이트"),
    TRUE_WINTER(Season.WINTER, "true", "cool", "트루"),
    DEEP_WINTER(Season.WINTER, "deep", "딥"),
    CLEAR_WINTER(Season.WINTER, "clear", "클리어");

    private final Season season;
    private final String[] toneKeywords;

    PersonalColorType(Season season, String... toneKeywords) {
        this.season = season;
        this.toneKeywords = toneKeywords;
    }

    public Season season() {
        return season;
    }

    /**
     * OCR로 추출한 자유 텍스트(한글/영문)에서 계절과 톤을 식별해 16종으로 매핑한다.
     *
     * @param text OCR 결과 텍스트 (null/blank 허용)
     * @return 계절과 톤을 모두 식별하면 해당 타입, 아니면 {@link Optional#empty()}
     */
    public static Optional<PersonalColorType> fromText(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String haystack = text.toLowerCase(Locale.ROOT);

        Season season = longestSeasonMatch(haystack);
        if (season == null) {
            return Optional.empty();
        }
        return longestToneMatch(haystack, season);
    }

    private static Season longestSeasonMatch(String haystack) {
        Season best = null;
        int bestLength = 0;
        for (Season season : Season.values()) {
            for (String keyword : season.keywords()) {
                if (haystack.contains(keyword) && keyword.length() > bestLength) {
                    best = season;
                    bestLength = keyword.length();
                }
            }
        }
        return best;
    }

    /** 주어진 계절에 속한 타입 중, 가장 긴 톤 키워드가 일치하는 타입을 고른다. */
    private static Optional<PersonalColorType> longestToneMatch(String haystack, Season season) {
        PersonalColorType best = null;
        int bestLength = 0;
        for (PersonalColorType type : values()) {
            if (type.season != season) {
                continue;
            }
            for (String keyword : type.toneKeywords) {
                if (haystack.contains(keyword) && keyword.length() > bestLength) {
                    best = type;
                    bestLength = keyword.length();
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /** 사계절. 한글 음차(스프링/서머/오텀/윈터)와 고유어(봄/여름/가을/겨울), 영문을 모두 수용. */
    public enum Season {
        SPRING("spring", "스프링", "봄"),
        SUMMER("summer", "서머", "여름"),
        AUTUMN("autumn", "fall", "오텀", "가을"),
        WINTER("winter", "윈터", "겨울");

        private final String[] keywords;

        Season(String... keywords) {
            this.keywords = keywords;
        }

        String[] keywords() {
            return keywords;
        }
    }
}
