/**
 * 한글 입력을 두벌식 키보드 기준 영문 키로 변환한다.
 *
 * IME가 한글 상태일 때 사용자가 누른 물리 키를 영문으로 되돌리는 용도.
 * 예) "비밀번호" → "qlalfqjsgh", "안녕" → "dkssud"
 *
 * 완성형 음절은 초성/중성/종성으로 분해하고, 단독 호환 자모는 직접 매핑한다.
 * 한글이 아닌 문자(영문·숫자·기호)는 그대로 통과시킨다.
 */

const HANGUL_BASE = 0xac00
const HANGUL_END = 0xd7a3
const JUNG_COUNT = 21
const JONG_COUNT = 28

// 초성 19자 (분해 인덱스 → 호환 자모)
const CHOSEONG = [
  'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
  'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ',
]

// 중성 21자
const JUNGSEONG = [
  'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ',
  'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ',
]

// 종성 28자 (인덱스 0 = 받침 없음)
const JONGSEONG = [
  '', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ',
  'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ',
  'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ',
]

// 호환 자모 → 두벌식 QWERTY 키. 겹자모/겹모음은 2키로 매핑.
const JAMO_TO_QWERTY: Record<string, string> = {
  // 자음
  ㄱ: 'r', ㄲ: 'R', ㄳ: 'rt', ㄴ: 's', ㄵ: 'sw', ㄶ: 'sg', ㄷ: 'e', ㄸ: 'E',
  ㄹ: 'f', ㄺ: 'fr', ㄻ: 'fa', ㄼ: 'fq', ㄽ: 'ft', ㄾ: 'fx', ㄿ: 'fv', ㅀ: 'fg',
  ㅁ: 'a', ㅂ: 'q', ㅃ: 'Q', ㅄ: 'qt', ㅅ: 't', ㅆ: 'T', ㅇ: 'd', ㅈ: 'w',
  ㅉ: 'W', ㅊ: 'c', ㅋ: 'z', ㅌ: 'x', ㅍ: 'v', ㅎ: 'g',
  // 모음
  ㅏ: 'k', ㅐ: 'o', ㅑ: 'i', ㅒ: 'O', ㅓ: 'j', ㅔ: 'p', ㅕ: 'u', ㅖ: 'P',
  ㅗ: 'h', ㅘ: 'hk', ㅙ: 'ho', ㅚ: 'hl', ㅛ: 'y', ㅜ: 'n', ㅝ: 'nj', ㅞ: 'np',
  ㅟ: 'nl', ㅠ: 'b', ㅡ: 'm', ㅢ: 'ml', ㅣ: 'l',
}

export function hangulToQwerty(input: string): string {
  let result = ''

  for (const char of input) {
    const code = char.codePointAt(0)

    if (code !== undefined && code >= HANGUL_BASE && code <= HANGUL_END) {
      const offset = code - HANGUL_BASE
      const choIndex = Math.floor(offset / (JUNG_COUNT * JONG_COUNT))
      const jungIndex = Math.floor((offset % (JUNG_COUNT * JONG_COUNT)) / JONG_COUNT)
      const jongIndex = offset % JONG_COUNT

      result += JAMO_TO_QWERTY[CHOSEONG[choIndex]]
      result += JAMO_TO_QWERTY[JUNGSEONG[jungIndex]]
      if (jongIndex > 0) {
        result += JAMO_TO_QWERTY[JONGSEONG[jongIndex]]
      }
      continue
    }

    result += JAMO_TO_QWERTY[char] ?? char
  }

  return result
}
