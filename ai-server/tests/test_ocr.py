"""OCR personal color extraction tests.
Extracts PersonalColorType from generated result image text via OpenAI Vision.
The season-scoped matching mirrors Java PersonalColorType.fromText() logic.
"""

import pytest

from app.services.ocr import extract_personal_color_type


class TestExtractPersonalColorType:
    """Season-scoped matching: detect season first, then tone within that season."""

    def test_light_summer_korean(self):
        assert extract_personal_color_type("여름 쿨 라이트 (Summer Cool Light)") == "LIGHT_SUMMER"

    def test_cool_summer_korean(self):
        assert extract_personal_color_type("여름 쿨 (Summer Cool)") == "COOL_SUMMER"

    def test_soft_summer_korean(self):
        assert extract_personal_color_type("여름 소프트 (Summer Soft)") == "SOFT_SUMMER"

    def test_true_summer_korean(self):
        assert extract_personal_color_type("여름 트루 (Summer True)") == "TRUE_SUMMER"

    def test_light_spring_korean(self):
        assert extract_personal_color_type("봄 웜 라이트 (Spring Warm Light)") == "LIGHT_SPRING"

    def test_bright_spring_korean(self):
        assert extract_personal_color_type("봄 브라이트 (Spring Bright)") == "BRIGHT_SPRING"

    def test_true_spring_korean(self):
        assert extract_personal_color_type("봄 트루 (Spring True Warm)") == "TRUE_SPRING"

    def test_clear_spring_korean(self):
        assert extract_personal_color_type("봄 클리어 (Spring Clear)") == "CLEAR_SPRING"

    def test_deep_autumn_korean(self):
        assert extract_personal_color_type("가을 딥 (Autumn Deep)") == "DEEP_AUTUMN"

    def test_soft_autumn_korean(self):
        assert extract_personal_color_type("가을 소프트 (Autumn Soft)") == "SOFT_AUTUMN"

    def test_muted_autumn_korean(self):
        assert extract_personal_color_type("가을 뮤트 (Autumn Muted)") == "MUTED_AUTUMN"

    def test_true_autumn_korean(self):
        assert extract_personal_color_type("가을 웜 트루 (Autumn True Warm)") == "TRUE_AUTUMN"

    def test_deep_winter_korean(self):
        assert extract_personal_color_type("겨울 딥 (Winter Deep)") == "DEEP_WINTER"

    def test_bright_winter_korean(self):
        assert extract_personal_color_type("겨울 브라이트 (Winter Bright)") == "BRIGHT_WINTER"

    def test_clear_winter_korean(self):
        assert extract_personal_color_type("겨울 클리어 (Winter Clear)") == "CLEAR_WINTER"

    def test_true_winter_korean(self):
        assert extract_personal_color_type("겨울 쿨 트루 (Winter True Cool)") == "TRUE_WINTER"

    def test_english_only(self):
        assert extract_personal_color_type("Summer Light") == "LIGHT_SUMMER"

    def test_case_insensitive(self):
        assert extract_personal_color_type("WINTER DEEP") == "DEEP_WINTER"

    def test_none_when_no_season(self):
        assert extract_personal_color_type("light") is None

    def test_none_when_empty(self):
        assert extract_personal_color_type("") is None

    def test_none_when_no_match(self):
        assert extract_personal_color_type("봄 알수없는톤") is None

    def test_bright_not_confused_with_light_in_spring(self):
        # "브라이트" contains "라이트" as substring — longest-match must pick BRIGHT
        assert extract_personal_color_type("봄 브라이트 (Spring Bright)") == "BRIGHT_SPRING"

    def test_cool_summer_not_confused_with_true_winter(self):
        # "cool" appears in COOL_SUMMER and TRUE_WINTER — season scope disambiguates
        assert extract_personal_color_type("summer cool") == "COOL_SUMMER"
        assert extract_personal_color_type("winter cool") == "TRUE_WINTER"
