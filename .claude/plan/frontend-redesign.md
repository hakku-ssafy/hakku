# 프론트엔드 전면 재설계 — 스위스 에디토리얼 모노크롬

## 목표
전체 기능을 100% 보존한 채 UI를 처음부터 재설계한다. 흰 배경 + 어두운 회색 그레이스케일, 포인트컬러 없음. 퍼스널컬러가 들어앉을 단일 액센트 토큰(`--color-accent`)을 비워두고, 진단 완료 시 런타임 치환한다.

## 방향
- 스위스 에디토리얼 모노크롬: 엄격한 그리드, 대형 타이포 위계, hairline 보더, 넉넉한 여백
- 타이포: Noto Serif KR (디스플레이/헤드라인) × Pretendard (본문/UI)
- 레퍼런스: neworigin.co.kr (절제·여백·프리미엄)

## 토큰 (style.css, Tailwind v4 @theme)
- bg #ffffff / surface-2 #fafafa / surface-3 #f4f4f5
- text #18181b / text-2 #52525b / text-3 #a1a1aa
- border #e7e7e9 / border-strong #d4d4d8
- accent = var, 기본 #18181b (모노크롬), 퍼스널컬러 치환 슬롯
- font-serif Noto Serif KR, font-sans Pretendard
- radius 절제, shadow 은은

## 동적 액센트
- composables/usePersonalColorTheme.ts + 16종 → hex 맵
- COMPLETED 시 documentElement에 --color-accent 주입, 그 외 모노크롬

## 컴포넌트 (components/ui/)
AppButton, AppCard, ProductCard, AppInput, AppTextarea, AppSelect, SectionHeader, AppBadge, AppModal, EmptyState, Skeleton, layout/AppHeader, layout/AppFooter

## 실행 순서
1. index.html 폰트 + style.css 토큰
2. UI 라이브러리 + 컴포저블
3. App.vue (헤더/푸터)
4. 뷰 14종 재설계 (API/스토어/라우터/props/핸들러 불변)
5. vue-tsc 타입체크 + vitest 통과
6. 리뷰(자체/gan-design): 접근성·반응형·일관성

## 제약
- 기존 테스트(LoginView, ProductListView) 깨지지 않게 텍스트/role/동작 보존
- 기능·API 일절 변경 금지
