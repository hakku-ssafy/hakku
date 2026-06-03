"""Personal color type extraction from OCR/Vision output text.

Mirrors Java PersonalColorType.fromText() — season-scoped matching:
1. Direct ENUM match (from dashboard label parentheses)
2. Full Korean label match (longest wins)
3. Season keyword, then tone keyword within that season (fallback)

The season-scope prevents cross-season ambiguity (e.g. "cool" in Summer vs Winter).
"""

from __future__ import annotations

import re
from typing import Optional

# Primary Korean labels rendered on the generated dashboard (enum, label)
PERSONAL_COLOR_LABELS: list[tuple[str, str]] = [
    ("LIGHT_SPRING", "봄 웜 라이트"),
    ("TRUE_SPRING", "봄 웜 트루"),
    ("BRIGHT_SPRING", "봄 브라이트"),
    ("CLEAR_SPRING", "봄 클리어"),
    ("LIGHT_SUMMER", "여름 쿨 라이트"),
    ("TRUE_SUMMER", "여름 트루"),
    ("SOFT_SUMMER", "여름 소프트"),
    ("COOL_SUMMER", "여름 쿨"),
    ("SOFT_AUTUMN", "가을 소프트"),
    ("TRUE_AUTUMN", "가을 웜 트루"),
    ("DEEP_AUTUMN", "가을 딥"),
    ("MUTED_AUTUMN", "가을 뮤트"),
    ("BRIGHT_WINTER", "겨울 브라이트"),
    ("TRUE_WINTER", "겨울 쿨 트루"),
    ("DEEP_WINTER", "겨울 쿨 딥"),
    ("CLEAR_WINTER", "겨울 클리어"),
]

_ALTERNATE_LABELS: list[tuple[str, str]] = [
    ("LIGHT_SPRING", "봄 라이트"),
    ("TRUE_SPRING", "봄 트루"),
    ("DEEP_WINTER", "겨울 딥"),
    ("TRUE_WINTER", "겨울 트루"),
]

_ENUM_RE = re.compile(
    r"\b("
    r"LIGHT_SPRING|TRUE_SPRING|BRIGHT_SPRING|CLEAR_SPRING|"
    r"LIGHT_SUMMER|TRUE_SUMMER|SOFT_SUMMER|COOL_SUMMER|"
    r"SOFT_AUTUMN|TRUE_AUTUMN|DEEP_AUTUMN|MUTED_AUTUMN|"
    r"BRIGHT_WINTER|TRUE_WINTER|DEEP_WINTER|CLEAR_WINTER"
    r")\b"
)

_SEASONS: list[tuple[str, list[str]]] = [
    ("SPRING", ["spring", "스프링", "봄"]),
    ("SUMMER", ["summer", "서머", "여름"]),
    ("AUTUMN", ["autumn", "fall", "오텀", "가을"]),
    ("WINTER", ["winter", "윈터", "겨울"]),
]

_TONES: dict[str, list[tuple[str, list[str]]]] = {
    "SPRING": [
        ("LIGHT_SPRING", ["light", "라이트"]),
        ("TRUE_SPRING", ["true", "warm", "트루", "웜"]),
        ("BRIGHT_SPRING", ["bright", "브라이트"]),
        ("CLEAR_SPRING", ["clear", "클리어"]),
    ],
    "SUMMER": [
        ("LIGHT_SUMMER", ["light", "라이트"]),
        ("TRUE_SUMMER", ["true", "트루"]),
        ("SOFT_SUMMER", ["soft", "소프트"]),
        ("COOL_SUMMER", ["cool", "쿨"]),
    ],
    "AUTUMN": [
        ("SOFT_AUTUMN", ["soft", "소프트"]),
        ("TRUE_AUTUMN", ["true", "warm", "트루", "웜"]),
        ("DEEP_AUTUMN", ["deep", "딥"]),
        ("MUTED_AUTUMN", ["muted", "mute", "뮤트"]),
    ],
    "WINTER": [
        ("BRIGHT_WINTER", ["bright", "브라이트"]),
        ("TRUE_WINTER", ["true", "cool", "트루"]),
        ("DEEP_WINTER", ["deep", "딥"]),
        ("CLEAR_WINTER", ["clear", "클리어"]),
    ],
}


def format_labels_for_generator_prompt() -> str:
    """Bullet list of allowed 세부 타입 labels for the image-generation prompt."""
    return "\n".join(
        f"  - 세부 타입: {ko} ({enum})" for enum, ko in PERSONAL_COLOR_LABELS
    )


def _all_korean_labels() -> list[tuple[str, str]]:
    combined = PERSONAL_COLOR_LABELS + _ALTERNATE_LABELS
    return sorted(combined, key=lambda pair: len(pair[1]), reverse=True)


def extract_personal_color_type(text: str) -> Optional[str]:
    """Return PersonalColorType enum name (e.g. 'LIGHT_SUMMER') or None."""
    if not text:
        return None

    enum_match = _ENUM_RE.search(text.upper())
    if enum_match:
        return enum_match.group(1)

    haystack = text.lower()

    best_label: Optional[str] = None
    best_label_len = 0
    for enum_name, label in _all_korean_labels():
        if label.lower() in haystack and len(label) > best_label_len:
            best_label = enum_name
            best_label_len = len(label)
    if best_label:
        return best_label

    season = _longest_season_match(haystack)
    if season is None:
        return None

    return _longest_tone_match(haystack, season)


def _longest_season_match(haystack: str) -> Optional[str]:
    best: Optional[str] = None
    best_len = 0
    for season_name, keywords in _SEASONS:
        for kw in keywords:
            if kw in haystack and len(kw) > best_len:
                best = season_name
                best_len = len(kw)
    return best


def _longest_tone_match(haystack: str, season: str) -> Optional[str]:
    best: Optional[str] = None
    best_len = 0
    for type_name, keywords in _TONES[season]:
        for kw in keywords:
            if kw in haystack and len(kw) > best_len:
                best = type_name
                best_len = len(kw)
    return best
