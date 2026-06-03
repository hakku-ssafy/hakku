"""Personal color type extraction from OCR/Vision output text.

Mirrors Java PersonalColorType.fromText() — season-scoped matching:
1. Identify season from text (longest keyword wins)
2. Within that season, find the tone keyword (longest wins)

The season-scope prevents cross-season ambiguity (e.g. "cool" in Summer vs Winter).
"""

from __future__ import annotations
from typing import Optional


_SEASONS: list[tuple[str, list[str]]] = [
    ("SPRING", ["spring", "스프링", "봄"]),
    ("SUMMER", ["summer", "서머", "여름"]),
    ("AUTUMN", ["autumn", "fall", "오텀", "가을"]),
    ("WINTER", ["winter", "윈터", "겨울"]),
]

_TONES: dict[str, list[tuple[str, list[str]]]] = {
    "SPRING": [
        ("LIGHT_SPRING",  ["light", "라이트"]),
        ("TRUE_SPRING",   ["true", "warm", "트루", "웜"]),
        ("BRIGHT_SPRING", ["bright", "브라이트"]),
        ("CLEAR_SPRING",  ["clear", "클리어"]),
    ],
    "SUMMER": [
        ("LIGHT_SUMMER",  ["light", "라이트"]),
        ("TRUE_SUMMER",   ["true", "트루"]),
        ("SOFT_SUMMER",   ["soft", "소프트"]),
        ("COOL_SUMMER",   ["cool", "쿨"]),
    ],
    "AUTUMN": [
        ("SOFT_AUTUMN",   ["soft", "소프트"]),
        ("TRUE_AUTUMN",   ["true", "warm", "트루", "웜"]),
        ("DEEP_AUTUMN",   ["deep", "딥"]),
        ("MUTED_AUTUMN",  ["muted", "mute", "뮤트"]),
    ],
    "WINTER": [
        ("BRIGHT_WINTER", ["bright", "브라이트"]),
        ("TRUE_WINTER",   ["true", "cool", "트루"]),
        ("DEEP_WINTER",   ["deep", "딥"]),
        ("CLEAR_WINTER",  ["clear", "클리어"]),
    ],
}


def extract_personal_color_type(text: str) -> Optional[str]:
    """Return PersonalColorType enum name (e.g. 'LIGHT_SUMMER') or None."""
    if not text:
        return None

    haystack = text.lower()

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
