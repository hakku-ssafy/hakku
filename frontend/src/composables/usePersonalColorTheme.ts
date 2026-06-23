/**
 * 퍼스널컬러 → 동적 액센트 테마.
 *
 * 디자인 시스템(docs/design-system/personal-color.md)은 액센트를 **3토큰**으로 둔다:
 *   --accent       : 메인 채도색(버튼·배지·큰 블록)
 *   --accent-soft  : 고명도 옅은 틴트(배경)
 *   --accent-ink   : 저명도 변형(--accent-soft 위 텍스트, 접근성 대비)
 * 이 세 토큰이 "퍼스널컬러가 들어앉는 자리"다. 미진단/비로그인이면 토큰을 비워
 * style.css :root 기본값(먹색 #16140f)으로 되돌아가, 색은 진단의 보너스로만 입혀진다.
 *
 * 진단이 COMPLETED 되면 사용자의 16종 세부 타입에 맞는 웜뮤트 3토큰을 :root 에 주입해
 * 전체 UI(버튼·게이지·헤더 PC 배지·찜 하트 등)가 본인 퍼스널컬러로 물든다.
 * Tailwind 의 bg-accent / text-accent / bg-accent-soft / text-accent-ink 유틸은
 * style.css @theme 의 `--color-accent: var(--accent)` alias 덕에 이 주입을 그대로 추종한다.
 */

interface PersonalColorAccent {
  /** 메인 채도색 */
  accent: string
  /** 옅은 틴트 배경 */
  accentSoft: string
  /** accent-soft 위 텍스트(저명도, 대비 확보) */
  accentInk: string
}

/**
 * 16종 퍼스널컬러 코드 → 웜뮤트 3토큰.
 * 계절 앵커는 디자인 시스템 문서값(봄 #e8a07e / 여름 #d98ba6 / 가을 #c0795a / 겨울 #8e7bc4),
 * 서브타입은 명도·채도로 변주하되 에디토리얼 뮤트 레지스터를 유지한다.
 * accent 면 텍스트는 흰색 대신 accentInk 를 써 대비를 확보한다(라이트 계열 가독성).
 */
const PERSONAL_COLOR_ACCENTS: Record<string, PersonalColorAccent> = {
  // 봄 — 따뜻하고 화사한(소프트 코랄·피치)
  LIGHT_SPRING: { accent: '#e8a07e', accentSoft: '#fbede3', accentInk: '#9c5733' },
  WARM_SPRING: { accent: '#e0926a', accentSoft: '#fae9dd', accentInk: '#98512f' },
  BRIGHT_SPRING: { accent: '#e79775', accentSoft: '#fceadf', accentInk: '#a1542f' },
  CLEAR_SPRING: { accent: '#e58a66', accentSoft: '#fbe6da', accentInk: '#9a4e2c' },

  // 여름 — 시원하고 부드러운(더스티 로즈·라벤더)
  LIGHT_SUMMER: { accent: '#c8a0c4', accentSoft: '#f4ecf4', accentInk: '#6f4a78' },
  COOL_SUMMER: { accent: '#b6a3cf', accentSoft: '#efebf6', accentInk: '#5a4a86' },
  SOFT_SUMMER: { accent: '#c79bb0', accentSoft: '#f6ecf1', accentInk: '#7c4866' },
  MUTED_SUMMER: { accent: '#d98ba6', accentSoft: '#fbe9f0', accentInk: '#9c4467' },

  // 가을 — 따뜻하고 깊은(테라코타·머스타드)
  SOFT_AUTUMN: { accent: '#b0865a', accentSoft: '#f3ebdf', accentInk: '#6c4a2a' },
  WARM_AUTUMN: { accent: '#c0795a', accentSoft: '#f6e6dc', accentInk: '#7a4030' },
  DEEP_AUTUMN: { accent: '#a86145', accentSoft: '#f1e2d8', accentInk: '#6a3826' },
  MUTED_AUTUMN: { accent: '#b08b54', accentSoft: '#f3ecdd', accentInk: '#6b4d28' },

  // 겨울 — 시원하고 깊은(뮤트 바이올렛·인디고)
  CLEAR_WINTER: { accent: '#7d76bf', accentSoft: '#e9e7f5', accentInk: '#423a7c' },
  COOL_WINTER: { accent: '#7b80b8', accentSoft: '#e9eaf3', accentInk: '#3d4079' },
  DEEP_WINTER: { accent: '#876a9e', accentSoft: '#efe7f1', accentInk: '#4d3560' },
  BRIGHT_WINTER: { accent: '#8e7bc4', accentSoft: '#ece7f7', accentInk: '#4f3f86' },
}

/** 진단 액센트가 주입하는 CSS 변수 목록 (리셋 시 제거 대상) */
const ACCENT_VARS = ['--accent', '--accent-soft', '--accent-ink'] as const

/**
 * 퍼스널컬러 코드에 해당하는 대표 액센트 hex 반환 (없으면 null).
 * 미리보기·배지 등에서 색을 직접 쓰고 싶을 때 사용.
 */
export function accentForPersonalColor(color: string | null): string | null {
  if (!color) return null
  return PERSONAL_COLOR_ACCENTS[color]?.accent ?? null
}

/**
 * documentElement 에 퍼스널컬러 3토큰을 주입한다.
 * color 가 매핑에 없거나 null 이면 토큰을 제거해 :root 기본(먹색)으로 복귀.
 */
export function applyPersonalColorTheme(color: string | null): void {
  if (typeof document === 'undefined') return
  const root = document.documentElement
  const theme = color ? PERSONAL_COLOR_ACCENTS[color] : undefined

  if (!theme) {
    for (const cssVar of ACCENT_VARS) root.style.removeProperty(cssVar)
    return
  }

  root.style.setProperty('--accent', theme.accent)
  root.style.setProperty('--accent-soft', theme.accentSoft)
  root.style.setProperty('--accent-ink', theme.accentInk)
}
