package com.hakku.main.personalcolor.domain;

import java.util.Locale;
import java.util.Optional;

/**
 * 16종 퍼스널컬러 분류 (4계절 × 4톤).
 *
 * <p>AI 진단 결과 이미지를 OCR로 읽어 추출한 텍스트를 이 enum 값으로 매핑한다
 * ({@link #fromText(String)}).
 */
public enum PersonalColorType {

    SPRING_LIGHT(Season.SPRING, Tone.LIGHT),
    SPRING_BRIGHT(Season.SPRING, Tone.BRIGHT),
    SPRING_MUTE(Season.SPRING, Tone.MUTE),
    SPRING_DEEP(Season.SPRING, Tone.DEEP),

    SUMMER_LIGHT(Season.SUMMER, Tone.LIGHT),
    SUMMER_BRIGHT(Season.SUMMER, Tone.BRIGHT),
    SUMMER_MUTE(Season.SUMMER, Tone.MUTE),
    SUMMER_DEEP(Season.SUMMER, Tone.DEEP),

    AUTUMN_LIGHT(Season.AUTUMN, Tone.LIGHT),
    AUTUMN_BRIGHT(Season.AUTUMN, Tone.BRIGHT),
    AUTUMN_MUTE(Season.AUTUMN, Tone.MUTE),
    AUTUMN_DEEP(Season.AUTUMN, Tone.DEEP),

    WINTER_LIGHT(Season.WINTER, Tone.LIGHT),
    WINTER_BRIGHT(Season.WINTER, Tone.BRIGHT),
    WINTER_MUTE(Season.WINTER, Tone.MUTE),
    WINTER_DEEP(Season.WINTER, Tone.DEEP);

    private final Season season;
    private final Tone tone;

    PersonalColorType(Season season, Tone tone) {
        this.season = season;
        this.tone = tone;
    }

    public Season season() {
        return season;
    }

    public Tone tone() {
        return tone;
    }

    /**
     * OCR로 추출한 자유 텍스트(한글/영문)에서 계절 키워드와 톤 키워드를 찾아
     * 해당하는 16종 퍼스널컬러로 매핑한다.
     *
     * @param text OCR 결과 텍스트 (null/blank 허용)
     * @return 계절과 톤을 모두 식별하면 해당 타입, 아니면 {@link Optional#empty()}
     */
    public static Optional<PersonalColorType> fromText(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String haystack = text.toLowerCase(Locale.ROOT);
        Season season = matchKeyword(haystack, Season.values(), Season::keywords);
        Tone tone = matchKeyword(haystack, Tone.values(), Tone::keywords);
        if (season == null || tone == null) {
            return Optional.empty();
        }
        for (PersonalColorType type : values()) {
            if (type.season == season && type.tone == tone) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /**
     * 가장 긴(=가장 구체적인) 키워드가 일치하는 후보를 고른다.
     * 예: "브라이트"(bright)는 "라이트"(light)를 부분 문자열로 포함하므로,
     * 더 긴 키워드 매칭을 우선해 BRIGHT 로 정확히 분류한다.
     */
    private static <E> E matchKeyword(String haystack, E[] candidates,
                                      java.util.function.Function<E, String[]> keywordsOf) {
        E best = null;
        int bestLength = 0;
        for (E candidate : candidates) {
            for (String keyword : keywordsOf.apply(candidate)) {
                if (haystack.contains(keyword) && keyword.length() > bestLength) {
                    best = candidate;
                    bestLength = keyword.length();
                }
            }
        }
        return best;
    }

    /** 사계절. */
    public enum Season {
        SPRING("spring", "봄"),
        SUMMER("summer", "여름"),
        AUTUMN("autumn", "fall", "가을"),
        WINTER("winter", "겨울");

        private final String[] keywords;

        Season(String... keywords) {
            this.keywords = keywords;
        }

        String[] keywords() {
            return keywords;
        }
    }

    /** 4톤. */
    public enum Tone {
        LIGHT("light", "라이트"),
        BRIGHT("bright", "브라이트"),
        MUTE("mute", "muted", "뮤트"),
        DEEP("deep", "딥");

        private final String[] keywords;

        Tone(String... keywords) {
            this.keywords = keywords;
        }

        String[] keywords() {
            return keywords;
        }
    }
}
