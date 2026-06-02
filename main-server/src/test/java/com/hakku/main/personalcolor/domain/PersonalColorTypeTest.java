package com.hakku.main.personalcolor.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PersonalColorTypeTest {

    @Test
    @DisplayName("정확히 16종 (4계절 × 4톤)")
    void hasSixteenTypes() {
        assertEquals(16, PersonalColorType.values().length);
    }

    @Test
    @DisplayName("영문 텍스트에서 계절+톤을 추출 (순서 무관)")
    void parsesEnglish() {
        assertEquals(Optional.of(PersonalColorType.SPRING_BRIGHT), PersonalColorType.fromText("Bright Spring"));
        assertEquals(Optional.of(PersonalColorType.SPRING_LIGHT), PersonalColorType.fromText("spring light"));
        assertEquals(Optional.of(PersonalColorType.WINTER_DEEP), PersonalColorType.fromText("Deep Winter"));
        assertEquals(Optional.of(PersonalColorType.AUTUMN_MUTE), PersonalColorType.fromText("muted autumn"));
    }

    @Test
    @DisplayName("한글 텍스트에서 계절+톤을 추출")
    void parsesKorean() {
        assertEquals(Optional.of(PersonalColorType.SPRING_BRIGHT), PersonalColorType.fromText("봄 브라이트"));
        assertEquals(Optional.of(PersonalColorType.SUMMER_MUTE), PersonalColorType.fromText("여름 뮤트"));
        assertEquals(Optional.of(PersonalColorType.AUTUMN_DEEP), PersonalColorType.fromText("가을 딥"));
        assertEquals(Optional.of(PersonalColorType.WINTER_LIGHT), PersonalColorType.fromText("겨울 라이트"));
    }

    @Test
    @DisplayName("enum 이름 형태(underscore)도 인식")
    void parsesEnumName() {
        assertEquals(Optional.of(PersonalColorType.WINTER_DEEP), PersonalColorType.fromText("WINTER_DEEP"));
    }

    @Test
    @DisplayName("문장 안에 섞여 있어도 추출 (OCR 결과 시뮬레이션)")
    void parsesWithinSentence() {
        String ocr = "진단 결과: 당신은 여름 뮤트 타입입니다.";
        assertEquals(Optional.of(PersonalColorType.SUMMER_MUTE), PersonalColorType.fromText(ocr));
    }

    @Test
    @DisplayName("계절은 있고 톤이 없으면 매핑 실패")
    void seasonOnlyIsEmpty() {
        assertTrue(PersonalColorType.fromText("봄").isEmpty());
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
