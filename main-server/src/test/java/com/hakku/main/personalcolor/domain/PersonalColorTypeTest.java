package com.hakku.main.personalcolor.domain;

import static com.hakku.main.personalcolor.domain.PersonalColorType.BRIGHT_SPRING;
import static com.hakku.main.personalcolor.domain.PersonalColorType.BRIGHT_WINTER;
import static com.hakku.main.personalcolor.domain.PersonalColorType.CLEAR_SPRING;
import static com.hakku.main.personalcolor.domain.PersonalColorType.CLEAR_WINTER;
import static com.hakku.main.personalcolor.domain.PersonalColorType.COOL_SUMMER;
import static com.hakku.main.personalcolor.domain.PersonalColorType.DEEP_AUTUMN;
import static com.hakku.main.personalcolor.domain.PersonalColorType.LIGHT_SPRING;
import static com.hakku.main.personalcolor.domain.PersonalColorType.MUTED_AUTUMN;
import static com.hakku.main.personalcolor.domain.PersonalColorType.SOFT_SUMMER;
import static com.hakku.main.personalcolor.domain.PersonalColorType.TRUE_SPRING;
import static com.hakku.main.personalcolor.domain.PersonalColorType.TRUE_SUMMER;
import static com.hakku.main.personalcolor.domain.PersonalColorType.TRUE_WINTER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PersonalColorTypeTest {

    @Test
    @DisplayName("정확히 16종")
    void hasSixteenTypes() {
        assertEquals(16, PersonalColorType.values().length);
    }

    @Test
    @DisplayName("한글 명칭에서 추출 (스프링/서머/오텀/윈터)")
    void parsesKoreanCanonicalNames() {
        assertEquals(Optional.of(LIGHT_SPRING), PersonalColorType.fromText("라이트 스프링"));
        assertEquals(Optional.of(TRUE_SPRING), PersonalColorType.fromText("트루 스프링"));
        assertEquals(Optional.of(CLEAR_SPRING), PersonalColorType.fromText("클리어 스프링"));
        assertEquals(Optional.of(TRUE_SUMMER), PersonalColorType.fromText("트루 서머"));
        assertEquals(Optional.of(COOL_SUMMER), PersonalColorType.fromText("쿨 서머"));
        assertEquals(Optional.of(SOFT_SUMMER), PersonalColorType.fromText("소프트 서머"));
        assertEquals(Optional.of(MUTED_AUTUMN), PersonalColorType.fromText("뮤트 오텀"));
        assertEquals(Optional.of(DEEP_AUTUMN), PersonalColorType.fromText("딥 오텀"));
        assertEquals(Optional.of(CLEAR_WINTER), PersonalColorType.fromText("클리어 윈터"));
        assertEquals(Optional.of(TRUE_WINTER), PersonalColorType.fromText("트루 윈터"));
    }

    @Test
    @DisplayName("한글 substring 충돌: '브라이트 스프링'은 라이트가 아니라 브라이트로")
    void koreanSubstringCollisionResolved() {
        assertEquals(Optional.of(BRIGHT_SPRING), PersonalColorType.fromText("브라이트 스프링"));
        assertEquals(Optional.of(BRIGHT_WINTER), PersonalColorType.fromText("브라이트 윈터"));
    }

    @Test
    @DisplayName("영문 명칭에서 추출 (순서 무관)")
    void parsesEnglishNames() {
        assertEquals(Optional.of(LIGHT_SPRING), PersonalColorType.fromText("Light Spring"));
        assertEquals(Optional.of(BRIGHT_WINTER), PersonalColorType.fromText("Bright Winter"));
        assertEquals(Optional.of(DEEP_AUTUMN), PersonalColorType.fromText("Deep Autumn"));
        assertEquals(Optional.of(SOFT_SUMMER), PersonalColorType.fromText("Soft Summer"));
        assertEquals(Optional.of(CLEAR_SPRING), PersonalColorType.fromText("clear spring"));
    }

    @Test
    @DisplayName("영문 별칭: Warm/Cool 은 해당 계절의 True 타입으로")
    void parsesEnglishAliases() {
        assertEquals(Optional.of(TRUE_SPRING), PersonalColorType.fromText("Warm Spring"));
        assertEquals(Optional.of(TRUE_WINTER), PersonalColorType.fromText("Cool Winter"));
    }

    @Test
    @DisplayName("계절별로 'cool' 은 스코프가 다르다: 여름→쿨서머, 겨울→트루윈터")
    void coolIsSeasonScoped() {
        assertEquals(Optional.of(COOL_SUMMER), PersonalColorType.fromText("Cool Summer"));
        assertEquals(Optional.of(TRUE_WINTER), PersonalColorType.fromText("Cool Winter"));
    }

    @Test
    @DisplayName("enum 이름 형태(underscore)도 인식")
    void parsesEnumName() {
        assertEquals(Optional.of(TRUE_WINTER), PersonalColorType.fromText("TRUE_WINTER"));
    }

    @Test
    @DisplayName("문장 안에 섞여 있어도 추출 (OCR 결과 시뮬레이션)")
    void parsesWithinSentence() {
        String ocr = "진단 결과: 당신은 뮤트 오텀 타입입니다.";
        assertEquals(Optional.of(MUTED_AUTUMN), PersonalColorType.fromText(ocr));
    }

    @Test
    @DisplayName("계절은 있고 톤이 없으면 매핑 실패")
    void seasonOnlyIsEmpty() {
        assertTrue(PersonalColorType.fromText("봄").isEmpty());
        assertTrue(PersonalColorType.fromText("스프링").isEmpty());
    }

    @Test
    @DisplayName("공백 / 무관한 텍스트는 empty")
    void unrecognizedIsEmpty() {
        assertTrue(PersonalColorType.fromText("").isEmpty());
        assertTrue(PersonalColorType.fromText("   ").isEmpty());
        assertTrue(PersonalColorType.fromText("hello world").isEmpty());
        assertTrue(PersonalColorType.fromText("퍼스널컬러").isEmpty());
    }

    @Test
    @DisplayName("null 입력은 empty (NPE 없음)")
    void nullIsEmpty() {
        assertTrue(PersonalColorType.fromText(null).isEmpty());
    }
}
