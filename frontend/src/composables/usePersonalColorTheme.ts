/**
 * 퍼스널컬러 → 동적 액센트 테마.
 *
 * 디자인 시스템은 기본적으로 비비드 핑크→바이올렛 그라데이션을 액센트로 쓰고,
 * `--color-accent` / `--color-accent-2` 한 쌍을 "퍼스널컬러가 들어앉는 자리"로 둔다.
 * 진단이 COMPLETED 되면 사용자의 16종 세부 타입에 맞는 비비드 2스톱을 주입해
 * 전체 UI(버튼·게이지·헤더 CTA·그라데이션 면)가 본인 퍼스널컬러로 물든다.
 * 미진단/비로그인은 토큰을 비워(@theme 기본값 = 브랜드 핑크→바이올렛) 되돌린다.
 *
 * accent-soft / accent-line 은 accent 를 color-mix 로 추종하므로
 * 시즌별 하드코딩 없이 런타임에서 파생값을 계산한다.
 */

/** 16종 퍼스널컬러 코드 → 비비드 2스톱(accent → accent2) + 대비 ink */
const PERSONAL_COLOR_ACCENTS: Record<
  string,
  { accent: string; accent2: string; ink: string }
> = {
  // 봄 — 따뜻하고 화사한 (코랄·피치·골든)
  LIGHT_SPRING: { accent: '#ff5c86', accent2: '#ffb073', ink: '#ffffff' },
  WARM_SPRING: { accent: '#f7642f', accent2: '#ffa838', ink: '#ffffff' },
  BRIGHT_SPRING: { accent: '#ff3d63', accent2: '#ff924a', ink: '#ffffff' },
  CLEAR_SPRING: { accent: '#f5324f', accent2: '#ff7a45', ink: '#ffffff' },

  // 여름 — 시원하고 부드러운 (라벤더·로즈·페리윙클)
  LIGHT_SUMMER: { accent: '#6e7cf5', accent2: '#c77be0', ink: '#ffffff' },
  COOL_SUMMER: { accent: '#5066f0', accent2: '#9e6be8', ink: '#ffffff' },
  SOFT_SUMMER: { accent: '#7e6ad6', accent2: '#c081c8', ink: '#ffffff' },
  MUTED_SUMMER: { accent: '#6370c4', accent2: '#a879c4', ink: '#ffffff' },

  // 가을 — 따뜻하고 깊은 (머스타드·테라코타·러스트)
  SOFT_AUTUMN: { accent: '#bc7836', accent2: '#d6a04c', ink: '#ffffff' },
  WARM_AUTUMN: { accent: '#be5827', accent2: '#e08a33', ink: '#ffffff' },
  DEEP_AUTUMN: { accent: '#9e4427', accent2: '#c06a2e', ink: '#ffffff' },
  MUTED_AUTUMN: { accent: '#94652f', accent2: '#b3853f', ink: '#ffffff' },

  // 겨울 — 시원하고 선명한 (마젠타·일렉트릭 블루·바이올렛)
  CLEAR_WINTER: { accent: '#2e63d6', accent2: '#7c4de0', ink: '#ffffff' },
  COOL_WINTER: { accent: '#3a52c8', accent2: '#6e4dd6', ink: '#ffffff' },
  DEEP_WINTER: { accent: '#7a2a9e', accent2: '#c0327e', ink: '#ffffff' },
  BRIGHT_WINTER: { accent: '#e01e63', accent2: '#8a2dd6', ink: '#ffffff' },
}

/** 진단 액센트가 주입하는 CSS 변수 목록 (리셋 시 제거 대상) */
const ACCENT_VARS = [
  '--color-accent',
  '--color-accent-2',
  '--color-accent-ink',
  '--color-accent-soft',
  '--color-accent-line',
] as const

/**
 * 퍼스널컬러 코드에 해당하는 대표 액센트 hex 반환 (없으면 null).
 * 미리보기·뱃지 등에서 색을 직접 쓰고 싶을 때 사용.
 */
export function accentForPersonalColor(color: string | null): string | null {
  if (!color) return null
  return PERSONAL_COLOR_ACCENTS[color]?.accent ?? null
}

/**
 * documentElement 에 퍼스널컬러 액센트 토큰을 주입한다.
 * color 가 매핑에 없거나 null 이면 토큰을 제거해 @theme 기본(브랜드 핑크→바이올렛)으로 복귀.
 */
export function applyPersonalColorTheme(color: string | null): void {
  if (typeof document === 'undefined') return
  const root = document.documentElement
  const theme = color ? PERSONAL_COLOR_ACCENTS[color] : undefined

  if (!theme) {
    for (const cssVar of ACCENT_VARS) root.style.removeProperty(cssVar)
    return
  }

  root.style.setProperty('--color-accent', theme.accent)
  root.style.setProperty('--color-accent-2', theme.accent2)
  root.style.setProperty('--color-accent-ink', theme.ink)
  // soft / line 은 accent 를 추종 (흰색과 혼합)
  root.style.setProperty('--color-accent-soft', `color-mix(in srgb, ${theme.accent} 12%, white)`)
  root.style.setProperty('--color-accent-line', `color-mix(in srgb, ${theme.accent} 24%, white)`)
}
