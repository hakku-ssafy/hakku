# 화면설계서 (UI / Screen Design Specification)

**프로젝트: 학꾸(Hakku) · 작성일: 2026-06-25 · 버전 1.0**

---

## 1. 개요

### 1.1 문서 목적
본 문서는 학꾸(Hakku) 서비스의 프런트엔드(Vue 3 + Vite + TypeScript + Tailwind + Pinia)가 제공하는 22개 화면(SCR-01 ~ SCR-22)과 전역 컴포넌트(학꾸AI 챗봇 FAB)의 정보구조(IA), 라우팅 규칙, 화면별 상세 명세를 정의한다. 모든 화면 식별자·라우트·접근권한은 팩트시트 §5의 정의를 따르며, 연동 API는 팩트시트 §6, 사용자 흐름은 팩트시트 §7을 근거로 한다. 각 화면의 구성요소·이벤트·연동 API는 `frontend/src/views/`의 실제 `.vue` 구현에서 추출하여 작성하였다.

### 1.2 화면 목록 (22개 SCR + 전역 ChatFab)

| ID | 화면(View) | 라우트 | 접근권한 | hideChatFab | 목적 |
|---|---|---|---|---|---|
| SCR-01 | HomeView | `/` | 공개 | - | 홈: 히어로 진단 캐러셀·매거진/추천·학생증 자랑 그리드·커뮤니티 프리뷰 |
| SCR-02 | LoginView | `/login` | 비로그인 전용(guestOnly) | O | 로그인(JWT) |
| SCR-03 | SignupView | `/signup` | 비로그인 전용(guestOnly) | O | 회원가입(NORMAL/SELLER) |
| SCR-04 | OnboardingView | `/onboarding` | 로그인(requiresAuth) | O | 온보딩: 선호 컬러 설정(NORMAL 필수) |
| SCR-05 | ProductListView | `/products` | 공개 | - | 상품 목록·검색·카테고리 필터·For You 추천 |
| SCR-06 | ProductDetailView | `/products/:id` | 공개 | - | 상품 상세·리뷰·찜·장바구니·결제 |
| SCR-07 | CommunityView | `/community` | 공개 | - | 자유게시판 + 학생증 자랑 탭 |
| SCR-08 | PostDetailView | `/community/:id` | 공개 | - | 게시글 상세·댓글·좋아요·연관 상품 |
| SCR-09 | ProfileView | `/users/:id` | 로그인(requiresAuth) | - | 공개 프로필: 팔로우·찜·리뷰·글 |
| SCR-10 | SellerProductsView | `/seller/products` | 로그인(requiresAuth, SELLER) | - | 판매자 상품 등록 |
| SCR-11 | DiagnosisView | `/diagnosis` | 로그인(requiresAuth) | - | AI 퍼스널컬러 진단 사진 업로드 |
| SCR-12 | CartView | `/cart` | 로그인(requiresAuth) | - | 장바구니 |
| SCR-13 | OrderFormView | `/order/new` | 로그인(requiresAuth) | O | 주문서 작성·배송지 입력 |
| SCR-14 | PaymentCheckoutView | `/payments/checkout` | 로그인(requiresAuth) | O | 토스페이먼츠 결제위젯 |
| SCR-15 | PaymentSuccessView | `/payments/success` | 로그인(requiresAuth) | O | 결제 성공·승인 |
| SCR-16 | PaymentFailView | `/payments/fail` | 공개 | O | 결제 실패 안내 |
| SCR-17 | RecommendationView | `/recommendations` | 로그인(requiresAuth) | - | 퍼스널컬러 맞춤 추천(점수 근거 표시) |
| SCR-18 | NotificationView | `/notifications` | 로그인(requiresAuth) | - | 알림함(폴링) |
| SCR-19 | MyPageView | `/my` | 로그인(requiresAuth) | - | 마이페이지: 진단 결과·주문·찜·리뷰·글·팔로우 |
| SCR-20 | MagazineDetailView | `/magazine/:id` | 공개 | - | 매거진 상세(마크다운·상품 임베드) |
| SCR-21 | AdminMagazineView | `/admin/magazine` | 로그인(requiresAuth, ADMIN) | - | 매거진 CRUD |
| SCR-22 | AdminProductsView | `/admin/products` | 로그인(requiresAuth, ADMIN) | - | 상품 전체 인라인 관리 |
| (전역) | ChatFab | 우측 하단 FAB | 거의 전 페이지(hideChatFab 제외) | - | 학꾸AI 고객센터 챗봇(SSE) |

> hideChatFab=O 인 화면(로그인·회원가입·온보딩·주문서·결제 플로우 전체)은 입력 집중을 위해 챗봇 FAB가 노출되지 않는다. 라우트 메타 `hideChatFab: true`로 제어되며, `ChatFab.vue`의 `isHidden` 계산속성이 이를 읽는다.

### 1.3 정보구조 (Information Architecture)

```d2
direction: down
root: "학꾸 (Hakku)" { style.fill: "#211C16"; style.font-color: "#ffffff"; style.bold: true }
nav: "주 내비게이션 (전역 헤더 / 모바일 하단탭)" {
  s01: "SCR-01 홈 /"; s05: "SCR-05 상품 /products"; s07: "SCR-07 커뮤니티 /community"
  s11: "SCR-11 진단 /diagnosis (로그인)"; s17: "SCR-17 추천 /recommendations (로그인)"; s19: "SCR-19 마이 /my (로그인)"
}
auth: "인증 영역" { s02: "SCR-02 로그인"; s03: "SCR-03 회원가입"; s04: "SCR-04 온보딩" }
commerce: "커머스 영역" {
  s06: "SCR-06 상품상세"; s12: "SCR-12 장바구니"; s13: "SCR-13 주문서"
  s14: "SCR-14 결제"; s15: "SCR-15 결제성공"; s16: "SCR-16 결제실패"; s10: "SCR-10 판매자상품 (SELLER)"
}
social: "커뮤니티/소셜 영역" { s08: "SCR-08 게시글상세"; s09: "SCR-09 프로필"; s18: "SCR-18 알림"; s20: "SCR-20 매거진상세" }
ai: "AI 영역" { fab: "전역 ChatFab (학꾸AI · SSE)" }
admin: "관리자 영역" { s21: "SCR-21 매거진관리 (ADMIN)"; s22: "SCR-22 상품관리 (ADMIN)" }
root -> nav; root -> auth; root -> commerce; root -> social; root -> ai; root -> admin
```

### 1.4 라우팅 가드 규칙

라우터(`frontend/src/router/index.ts`)의 전역 `beforeEach` 가드가 모든 화면 전환 시 다음 4종 규칙을 순차 적용한다.

| 가드 | 메타 키 | 적용 화면 | 동작 |
|---|---|---|---|
| 인증 필요 | `requiresAuth: true` | SCR-04, 09, 10, 11, 12, 13, 14, 15, 17, 18, 19, 21, 22 | 미인증 접근 시 `/login?redirect={to.fullPath}` 로 리다이렉트(로그인 후 원위치 복귀) |
| 비로그인 전용 | `guestOnly: true` | SCR-02, 03 | 이미 인증된 사용자가 접근하면 `/` 로 리다이렉트 |
| 사용자 정보 보강 | (공통) | requiresAuth 통과 전 전역 | 토큰은 있으나 `user`가 비어 있으면 `fetchMe()` 호출, 실패 시 `logout()` 후 `/login` |
| 관리자 전용 | `requiresAdmin: true` | SCR-21, 22 | 역할이 ADMIN이 아니면 `/` 로 리다이렉트 (백엔드 `/api/admin/**` 가 1차 방어선) |
| 온보딩 게이트 | (역할·플래그 기반) | 보호경로 전반 | `role === 'NORMAL' && !onboardingCompleted` 이면 보호경로(`/`, `/community`, `/products`, `/cart`, `/recommendations`, `/notifications`, `/my`, `/diagnosis` 및 그 하위) 접근 시 `/onboarding` 으로 강제 |

> 온보딩 게이트 판정 함수 `needsOnboarding(user)`는 `user?.role === 'NORMAL' && !user.onboardingCompleted` 를 반환한다. SELLER/ADMIN 및 온보딩 완료 NORMAL은 게이트를 통과한다. 보호경로 목록은 라우터에 명시된 `protectedPaths` 배열을 그대로 따른다(상품 상세 `/products/:id` 등은 `startsWith('/products')` 로 포함됨).

---

## 2. 화면 흐름도

### 2.1 전체 사용자 여정 (인증·온보딩 → 진단 → 추천·쇼핑·결제 → 커뮤니티·알림)

```d2
direction: down
visit: "방문" { shape: circle }
home: "SCR-01 홈"
browse: "공개 탐색" { shape: diamond }
plist: "SCR-05 상품목록"; comm: "SCR-07 커뮤니티"; mag: "SCR-20 매거진상세"
login: "SCR-02 로그인"; signup: "SCR-03 회원가입"; onb: "SCR-04 온보딩"; sell: "SCR-10 판매자상품"
diag: "SCR-11 진단"
pending: "PENDING · 분석중" { shape: cylinder }
done: "COMPLETED · 16종 퍼스널컬러" { shape: cylinder }
rec: "SCR-17 추천"; pdetail: "SCR-06 상품상세"; cart: "SCR-12 장바구니"
orderform: "SCR-13 주문서"; checkout: "SCR-14 결제"; success: "SCR-15 결제성공"; fail: "SCR-16 결제실패"
myorders: "SCR-19 마이 · 주문내역"
podetail: "SCR-08 게시글상세"; profile: "SCR-09 프로필"; notif: "SCR-18 알림"
chat: "ChatFab 학꾸AI · SSE" { shape: cylinder }
visit -> home
home -> browse: 비회원 둘러보기
browse -> plist; browse -> comm; browse -> mag
home -> login: 회원가입/로그인
login -> signup; signup -> onb: NORMAL; signup -> sell: SELLER
login -> onb: NORMAL & 미완료; login -> home: 온보딩 완료; onb -> home
home -> diag: AI 진단
diag -> pending: 사진 업로드 202
pending -> done: 폴링 COMPLETED + 알림
done -> rec; home -> rec
plist -> pdetail; rec -> pdetail
pdetail -> cart: 장바구니
pdetail -> checkout: 바로결제 (PRODUCT)
cart -> orderform: 주문하기
orderform -> checkout: 주문생성 (ORDER)
checkout -> success: 토스 인증 성공
checkout -> fail: 실패/취소
success -> myorders: 승인확정 + ORDER 알림
fail -> cart
comm -> podetail; podetail -> profile: 작성자
podetail -> notif: 댓글·좋아요 알림
profile -> notif: 팔로우 알림
done -> notif; success -> notif
home -> chat: 전역 {style.stroke-dash: 3}
```

### 2.2 진단 상태머신 (NONE → PENDING → COMPLETED, 실패 시 NONE)

```d2
direction: down
start: "" { shape: circle; style.fill: "#211C16"; width: 26; height: 26 }
NONE: "NONE\n미요청 · 가입 기본값" { style.font-size: 24; style.fill: "#EFEAE0"; style.stroke: "#9C9384" }
PENDING: "PENDING\n분석 진행중 · 슬롯 잠금" { style.font-size: 24; style.fill: "#FBE6C0"; style.stroke: "#D9912F" }
COMPLETED: "COMPLETED\n16종 퍼스널컬러 확정" { style.font-size: 24; style.fill: "#D7E8CC"; style.stroke: "#5E9C4C" }
done: "" { shape: circle; style.fill: "#211C16"; width: 26; height: 26 }
start -> NONE
NONE -> PENDING: "POST /diagnosis-request (요청 수락 · 슬롯 잠금)"
PENDING -> COMPLETED: "PATCH /personal-color (ENUM 파싱 성공 + Kafka)"
PENDING -> NONE: "DELETE /diagnosis-request (파이프라인 실패 복구)"
COMPLETED -> PENDING: "재진단 요청"
COMPLETED -> done
```

---

## 3. 화면 상세 명세

> 각 화면 명세는 실제 구현 SFC의 `<template>` 루트 구조, 주요 섹션/컴포넌트, `<script setup>`의 API/스토어 호출을 근거로 작성하였다. 와이어프레임은 레이아웃 스케치이며 실제 픽셀 치수가 아니다.

---

### SCR-01 · HomeView (홈)

- **라우트 / 접근권한**: `/` · 공개
- **목적**: 진단 상태에 따라 표정을 바꾸는 히어로 캐러셀을 정점으로, 상품(또는 맞춤 추천)·학생증 자랑·커뮤니티를 한눈에 노출하는 진입 허브.
- **관련 FR/UC**: FR-01(회원/인증 상태 표시), FR-02(진단 상태 카드), FR-03(상품 노출), FR-05(맞춤 추천), FR-06(학생증 자랑·커뮤니티 프리뷰), FR-08(매거진) / UC-01 서비스 탐색, UC-05 추천 확인

**주요 구성요소** (`HomeView.vue` template 확인)
- `HeroCarousel` — `diagnosis-state`(guest/none/pending/done), `personal-color-label`, `recommended-products` 를 받아 진단 상태별 히어로 표현. `view-image` 이벤트로 진단 이미지 모달 오픈.
- 키워드 퀵링크 칩 섹션(`KEYWORD_CHIPS`: 핀뱃지·키링·꾸미기 스티커·그립톡·다꾸·키캡·마스킹테이프 → `/products`, 퍼스널컬러 → `/diagnosis`).
- 상품/For You 섹션 — `SectionHeader`(진단 완료+추천 보유 시 eyebrow "For You"/제목 "맞춤 추천 상품", 그 외 "Shop"/"상품"), `ProductCard` 8개 그리드(2→3→4열), 추천 시 `AppBadge`(추천 ✦) 슬롯. 로딩은 `SkeletonBlock`, 비어 있으면 `EmptyState`.
- 학생증 자랑 격자 — `STUDENT_ID` 게시판에서 이미지 보유 글 상위 8개를 3:4 카드 그리드로(`showcasePosts`), "더보기 →" 는 `/community?board=showcase`.
- 커뮤니티 프리뷰 — `postStore.posts` 상위 5개를 border-top 리스트 행으로(제목·발췌·작성자·시간·♥·💬).
- 진단 이미지 모달 — `AppModal`, 인증이 필요한 storage 이미지를 `useAuthedImage` 로 토큰 fetch 후 object URL 표시.

**사용자 동작 / 이벤트**
- 칩/전체보기/더보기 클릭 → 각 라우트 이동.
- 히어로 "진단 이미지 보기" → `showDiagnosisModal = true`.
- 진단 상태가 `COMPLETED` 로 전환되면(폴링 반영 포함) `watch`가 추천을 자동 로드.

**연동 API**
- `GET /api/posts` (커뮤니티 프리뷰, `postStore.fetchPosts`)
- `GET /api/posts?board=STUDENT_ID` (학생증 자랑, `getPosts('STUDENT_ID')`)
- `GET /api/products` (`productStore.fetchProducts`)
- `GET /api/recommendations` (진단 완료 시, `loadRecommendations`)
- `GET /storage/images` (진단 이미지, JWT 첨부)

**와이어프레임**
```text
┌──────────────────────────────────────────────────────┐
│  [MarqueeStrip]  ───────────────────────────────────  │
│  [AppHeader: 로고 | 홈 상품 커뮤니티 진단 추천 | 🔍 PC배지 🔔 👤 🛒]│
├──────────────────────────────────────────────────────┤
│  ╔══════════ HERO CAROUSEL (진단 상태별) ══════════╗   │
│  ║  [guest]가입유도 / [none]진단하기 / [pending]분석중 ║   │
│  ║  / [done] 퍼스널컬러 + 추천 상품 4 + 진단이미지보기║   │
│  ╚══════════════════════════════════════════════════╝   │
│  [✦핀뱃지][✦키링][✦그립톡][✦다꾸] … [✦퍼스널컬러]      │
│  ── Shop / For You ──────────────── 전체보기 → ─────    │
│  [card][card][card][card]                              │
│  [card][card][card][card]                              │
│  ── Show off 학생증 자랑 ───────────── 더보기 → ────    │
│  [3:4][3:4][3:4][3:4]                                  │
│  ── Community 지금 학꾸 라운지 ──────── 더보기 → ────    │
│  · 글제목                      작성자 · 1일전 ♥12 💬3   │
│  · 글제목                      작성자 · 어제  ♥ 7 💬1   │
├──────────────────────────────────────────────────────┤
│  [AppFooter]                          [✦ AI 도우미 FAB]│
└──────────────────────────────────────────────────────┘
```

---

### SCR-11 · DiagnosisView (AI 퍼스널컬러 진단)

- **라우트 / 접근권한**: `/diagnosis` · 로그인(requiresAuth)
- **목적**: 얼굴 사진을 업로드해 16종 퍼스널컬러를 AI로 진단하고, 진단 상태(NONE/PENDING/COMPLETED)에 따라 화면을 전환한다.
- **관련 FR/UC**: FR-02-01(사진 업로드), FR-02-02(202 즉시반환·비동기 분석), FR-02-03(상태 폴링), FR-02-04(결과 표시·재진단), FR-07(완료 알림) / UC-02 AI 진단

**주요 구성요소** (`DiagnosisView.vue` template — `diagnosisStatus` 기반 4분기)
- 헤더 — eyebrow "AI Personal Color", 제목 "퍼스널컬러 진단", 안내 문구.
- (PENDING) 분석 중 카드 — `AppCard` + `hk-spinner--lg` 로더 + "AI가 분석하고 있어요 … 완료되면 알림으로 알려드릴게요".
- (COMPLETED) 결과 카드 — accent 그라데이션 블록 + 퍼스널컬러 라벨(`formatPersonalColor`) + "진단 이미지 보기"(`AppModal`) + "추천 상품 보기 →"(`/recommendations`).
- (로컬 submitted) 접수 완료 카드 — 체크 아이콘 + "진단 요청이 접수되었어요".
- (NONE) 업로드 드롭존 — `hk-dropzone`(1.5px dashed), 드래그&드롭/파일 선택, 미리보기, 클라이언트 검증(이미지 타입, 10MB 이하), 에러 알럿, "진단 시작" `AppButton`.

**사용자 동작 / 이벤트**
- 파일 드래그/선택 → `setFile()` 검증 후 `previewUrl` 생성.
- "진단 시작" → `runDiagnosis()`: 토큰을 직접 헤더에 실어 `aiClient.post('/api/diagnosis', formData)` 호출 → 성공 시 `submitted=true`, `fetchMe()` 로 상태 PENDING 반영. 409 응답이면 "이미 진단 요청이 접수되었습니다." 표시.
- 이후 App.vue 폴링이 PENDING→COMPLETED 를 감지해 화면이 결과 카드로 자동 전환.

**연동 API**
- `POST /ai/api/diagnosis` (multipart `image`, 202 즉시 반환, ai-server)
- `GET /api/users/me` (`authStore.fetchMe`, 상태 동기화)
- `GET /storage/images?kind=result` (결과 이미지, JWT ownerId 접근제어)

**와이어프레임**
```text
┌──────────── /diagnosis ────────────┐
│        AI Personal Color           │
│        퍼스널컬러 진단              │
│  얼굴이 잘 보이는 사진을 올리면…    │
│                                    │
│  [NONE] ┌──────────────────────┐   │
│         │   ◌  (점선 드롭존)    │   │
│         │ 사진을 드래그/클릭     │   │
│         │ JPG PNG WEBP ≤10MB    │   │
│         │   [ 파일 선택 ]       │   │
│         └──────────────────────┘   │
│         [ ▌ 진단 시작 ▌ ]          │
│                                    │
│  [PENDING] ◯ 스피너  AI가 분석…    │
│  [COMPLETED] ▓ 블록  TRUE_AUTUMN   │
│              [진단이미지][추천보기→]│
└────────────────────────────────────┘
```

---

### SCR-06 · ProductDetailView (상품 상세)

- **라우트 / 접근권한**: `/products/:id` · 공개 (찜·장바구니·리뷰·결제 액션은 로그인 필요, 미인증 시 로그인 유도)
- **목적**: 상품 정보·컬러·평점·리뷰를 보여주고 장바구니/즉시결제/찜/리뷰 작성을 수행한다.
- **관련 FR/UC**: FR-03-02(상품 상세), FR-03-03(리뷰 CRUD), FR-03-04(찜 토글), FR-03-05(장바구니 담기), FR-04-01(즉시 결제 진입) / UC-03 상품 조회·구매, UC-06 리뷰 작성

**주요 구성요소** (`ProductDetailView.vue` template)
- 뒤로가기 링크(`← 상품 목록으로`), 로딩 스피너/에러 분기.
- 2열 그리드(`pd__grid`, ≥900px) — 좌: 갤러리(이미지 또는 톤 그라데이션 플레이스홀더 + `NO.{code}`), 우: 정보.
- 정보 영역 — 카테고리 eyebrow, 상품명, 평점(`StarRating` + 평균/리뷰수), 가격, 설명, 컬러 칩 리스트(`colorHex` 도트 + 라벨).
- CTA 행 — "장바구니 담기"(`addToCart`), "결제하기"(`goToCheckout`), 찜 토글 버튼(♥/♡ + 카운트, 낙관적 업데이트). 장바구니 결과 메시지(`pd__cart-msg`).
- 리뷰 섹션 — 평균 평점, 내 리뷰 작성/수정 폼(`StarRating` editable + `AppTextarea`, `canWrite` 제어), 내 리뷰(`ReviewItem` can-manage, edit/delete), 타인 리뷰 목록, 비어 있으면 `EmptyState`. 미인증 시 로그인 안내.

**사용자 동작 / 이벤트**
- 장바구니/결제/찜/리뷰 작성: 미인증이면 `loginLink`(`/login?redirect={fullPath}`)로 이동.
- "결제하기" → `/payments/checkout?refType=PRODUCT&refId={id}&amount={price}&name={name}`.
- 찜 토글 → 낙관적 업데이트 후 `toggleWishlist` 결과로 정정.
- 리뷰 등록/수정/삭제 → 목록 즉시 반영.

**연동 API**
- `GET /api/products/{id}` (`getProduct`)
- `GET /api/products/{id}/reviews` (`getReviews`)
- `GET /api/products/{id}/wishlist` (`getWishlistStatus`)
- `POST /api/products/{id}/wishlist` (`toggleWishlist`)
- `POST /api/products/{id}/reviews` · `PUT /api/reviews/{id}` · `DELETE /api/reviews/{id}` (`createReview`/`updateReview`/`deleteReview`)
- `POST /api/cart/items` (`addCartItem`)

**와이어프레임**
```text
┌────────────────── /products/:id ──────────────────┐
│ ← 상품 목록으로                                     │
│ ┌────────────┐  CATEGORY                            │
│ │            │  상품명                              │
│ │  갤러리    │  ★★★★☆ 평균 4.2 · 리뷰 12             │
│ │  (3:4)     │  12,000원                            │
│ │            │  설명 텍스트……                       │
│ └────────────┘  COLOR ● 핑크 ● 블루                 │
│                 [장바구니 담기][결제하기][♡ 7]       │
│ ──────────────────────────────────────────────     │
│ 리뷰 12                                ★★★★☆ 4.2     │
│ ┌ 리뷰 작성 ★★★★★ [텍스트] [등록] ┐                 │
│ [내 리뷰] [타인 리뷰] [타인 리뷰] …                  │
└────────────────────────────────────────────────────┘
```

---

### SCR-17 · RecommendationView (맞춤 추천)

- **라우트 / 접근권한**: `/recommendations` · 로그인(requiresAuth)
- **목적**: 퍼스널컬러·선호·활동 기반 추천 상품을 점수 게이지와 추천 사유와 함께 제시(설명가능 추천).
- **관련 FR/UC**: FR-05-01(추천 목록 조회), FR-05-02(점수 분해 표시), FR-05-03(미진단 유도) / UC-05 추천 확인

**주요 구성요소** (`RecommendationView.vue` template)
- `SectionHeader`(eyebrow "For You", 제목 "맞춤 추천 상품", 설명).
- 퍼스널컬러 컨텍스트 알약(`rec-pc`) — 보유 시 "{퍼스널컬러} 기준으로 정렬했어요".
- 추천 그리드 — `ProductCard` 마다 `#meta` 슬롯에 `AppBadge`(추천 ✦), 점수 배지(`scorePercent`), 점수 게이지 바, 추천 사유 한 줄(`reasonLine` — breakdown 최고 기여 항목을 `REASON_LABELS`로 환산: 퍼스널컬러/선호컬러/스타일/인기/리뷰/활동).
- 비어 있으면 `EmptyState` + "퍼스널컬러 진단받기"(`/diagnosis`).

**사용자 동작 / 이벤트**
- 진입 시 `onMounted`에서 추천 로드. 카드 클릭 → 상품 상세.
- 점수 게이지는 현재 목록 내 최고 점수를 100%로 정규화(절대 스케일 비의존).

**연동 API**
- `GET /api/recommendations` (점수 + breakdown 포함)

**와이어프레임**
```text
┌────────────── /recommendations ──────────────┐
│ For You                                       │
│ 맞춤 추천 상품                                 │
│ (● TRUE_AUTUMN 기준으로 정렬했어요)            │
│ [card]          [card]          [card]  [card]│
│  추천✦ 92점      추천✦ 88점      …            │
│  ▓▓▓▓▓▓▓░░       ▓▓▓▓▓▓░░░                    │
│  퍼스널컬러가 잘  선호하는 컬러예요             │
│  맞아요                                        │
└────────────────────────────────────────────────┘
```

---

### SCR-14 · PaymentCheckoutView (토스페이먼츠 결제)

- **라우트 / 접근권한**: `/payments/checkout` · 로그인(requiresAuth) · hideChatFab
- **목적**: 결제 의도를 서버에 선등록(intent)한 뒤 토스페이먼츠 결제위젯을 렌더링하고 결제를 요청한다.
- **관련 FR/UC**: FR-04-02(결제 의도 선등록), FR-04-03(토스 위젯 결제 요청) / UC-04 결제

**주요 구성요소** (`PaymentCheckoutView.vue` script/template)
- 쿼리 파라미터 수신 — `refType`(PRODUCT|ORDER), `refId`, `amount`, `name(orderName)`.
- 진입 시 `prepareTossPayment({referenceType, referenceId, amount})` → `orderId` 수령.
- 토스 SDK 초기화 — `loadTossPayments(CLIENT_KEY)` → `widgets({customerKey: ANONYMOUS})` → `setAmount` → `renderPaymentMethods('#toss-payment-method')` + `renderAgreement('#toss-agreement')`.
- 본문 — 주문 요약(주문명·금액), 결제수단 위젯, 약관 위젯, "{금액}원 결제하기" 버튼, "샌드박스 결제 — 실제로 청구되지 않습니다" 안내.
- 위젯 대상 div는 `v-show`로 항상 DOM에 존재(렌더 시점 보장).

**사용자 동작 / 이벤트**
- "결제하기" → `widgets.requestPayment({orderId, orderName, successUrl, failUrl})`. 인증 성공 시 `/payments/success`, 실패 시 `/payments/fail` 로 리다이렉트(쿼리로 paymentKey/orderId/amount 전달).
- 결제 정보 누락(refId 없음 또는 amount≤0) 시 에러 메시지.

**연동 API**
- `POST /api/payments` (intent 선등록, payment-server `prepareTossPayment`)
- 토스페이먼츠 SDK(`@tosspayments/tosspayments-sdk`, 클라이언트 위젯)

**와이어프레임**
```text
┌──────────── /payments/checkout ────────────┐
│ ← 돌아가기                                   │
│ 결제                                         │
│ ───────────────────────────────────         │
│ 주문 N건                          24,000원   │
│ ┌─ 결제수단 위젯 (#toss-payment-method) ─┐   │
│ │  카드 / 간편결제 / 계좌이체 …          │   │
│ └────────────────────────────────────────┘   │
│ ┌─ 약관 동의 위젯 (#toss-agreement) ─────┐   │
│ └────────────────────────────────────────┘   │
│ [        24,000원 결제하기        ]          │
│ 샌드박스 결제 — 실제로 청구되지 않습니다.    │
└──────────────────────────────────────────────┘
```

---

### SCR-08 · PostDetailView (게시글 상세)

- **라우트 / 접근권한**: `/community/:id` · 공개 (좋아요·댓글은 로그인 필요)
- **목적**: 게시글 본문·이미지·연관 상품을 보여주고 좋아요/댓글 상호작용을 제공한다.
- **관련 FR/UC**: FR-06-02(게시글 상세), FR-06-03(좋아요 토글), FR-06-04(댓글 작성), FR-06-05(연관 상품 이동), FR-07(COMMENT/LIKE 알림 생성) / UC-06 커뮤니티 활동

**주요 구성요소** (`PostDetailView.vue` template)
- 뒤로가기(`커뮤니티로`), 로딩/에러 분기.
- 게시글 article — 제목, 메타(아바타 이니셜 + 작성자 링크 `/users/:authorId` + 시간), 좋아요 알약(`like-pill`, `HeartIcon`, 인증 시 토글/비인증 시 정적), 본문 이미지, 본문 텍스트(whitespace-pre-wrap).
- 관련 상품 섹션 — `post.relatedProducts` 를 2~3열 카드 그리드로(`/products/:id` 링크).
- 댓글 섹션 — 댓글 수, 인라인 입력 폼(`textarea` + "등록" `AppButton`, 인증 시), 비인증 시 로그인 안내, 댓글 목록(아바타 + 작성자 링크 + 시간 + 본문).

**사용자 동작 / 이벤트**
- 좋아요 → `postStore.toggleLikeAction(id)` 후 likeCount/liked 갱신.
- 댓글 등록 → `createComment(postId, content)` 후 목록에 추가.
- 작성자/연관상품 클릭 → 프로필/상품상세 이동.

**연동 API**
- `GET /api/posts/{id}` (`getPost`)
- `GET /api/posts/{id}/comments` (`getComments`)
- `POST /api/posts/{id}/comments` (`createComment`)
- `POST /api/posts/{id}/likes` (좋아요 토글, `postStore.toggleLikeAction`)

**와이어프레임**
```text
┌──────────── /community/:id ────────────┐
│ ← 커뮤니티로                            │
│ 게시글 제목                             │
│ (A) 작성자 · 2026.06.20                 │
│ [ ♥ 좋아요 12 ]                         │
│ [   본문 이미지 (선택)   ]              │
│ 본문 텍스트 ……                         │
│ ── 관련 상품 ──                         │
│ [상품][상품][상품]                      │
│ ── 댓글 5 ──                            │
│ [ 댓글을 입력하세요          ] [등록]   │
│ (A) 작성자 · 1일전                      │
│     댓글 내용 ……                       │
└──────────────────────────────────────────┘
```

---

### SCR-19 · MyPageView (마이페이지)

- **라우트 / 접근권한**: `/my` · 로그인(requiresAuth)
- **목적**: 내 퍼스널컬러·프로필·주문·리뷰·찜·글·팔로우를 탭으로 통합 관리한다.
- **관련 FR/UC**: FR-01-04(프로필 조회/수정), FR-01-05(선호 컬러 변경), FR-02-04(진단 결과 확인), FR-03-03/04(리뷰·찜 내역), FR-04-04(주문 내역), FR-06(내 글), FR-01-06(팔로우 관리) / UC-01, UC-04, UC-06

**주요 구성요소** (`MyPageView.vue` template)
- 헤더(eyebrow "My Page", 제목 "마이페이지").
- 퍼스널컬러 카드 — 진단 시 accent-soft, 미진단 시 안내 + "AI 진단 시작"/"재진단"(`/diagnosis`).
- 언더라인 탭(D1) 6개 — 프로필 / 주문 내역 / 리뷰 / 찜 / 내 글 / 팔로우. 각 탭은 lazy 로드(`selectTab` 내 `loaded` 플래그).
- 프로필 탭 — 아바타·닉네임·이메일, 역할 배지, 퍼스널컬러(클릭 시 진단 이미지 모달), 선호 컬러(변경 모달), 선호 스타일, ADMIN이면 "매거진 관리"/"상품 관리" 링크, 로그아웃.
- 주문 내역 탭 — 주문 카드(상태 배지 CREATED/PAID/CANCELLED, 날짜, 아이템 라인, 받는분·주소, 합계).
- 리뷰 / 찜(`WishlistCard`) / 내 글 / 팔로우(팔로워·팔로잉 서브탭, `UserListItem`) 탭. 각 탭 비어 있으면 `EmptyState`, 로딩은 `SkeletonList`.
- 모달 — 진단 이미지(`useAuthedImage`), 선호 컬러 변경(`updatePreferredColors`).

**사용자 동작 / 이벤트**
- 탭 전환 → 해당 데이터 lazy fetch. `?tab=orders`(알림에서 진입) / `?view=diagnosis` 쿼리로 탭·모달 자동 활성.
- 선호 컬러 저장 → 즉시 `authStore.user` 동기화. 로그아웃 → `authStore.logout()` 후 홈.

**연동 API**
- `GET /api/users/me` (`fetchMe`)
- `GET /api/orders` (`getMyOrders`)
- `GET /api/users/{id}/reviews` (`getUserReviews`)
- `GET /api/users/{id}/wishlist` (`getUserWishlist`)
- `GET /api/posts?author={id}` (`getPostsByAuthor`)
- `GET /api/users/{id}/followers` · `GET /api/users/{id}/following` (`getFollowers`/`getFollowing`)
- `PATCH /api/users/me/preferred-colors` (`updatePreferredColors`)

**와이어프레임**
```text
┌──────────────── /my ────────────────┐
│ My Page · 마이페이지                  │
│ ┌ Personal Color ─ TRUE_AUTUMN ─ 재진단→┐│
│ [프로필][주문내역][리뷰][찜][내 글][팔로우]│
│ ─────────── (활성 탭 내용) ───────────  │
│ 프로필: (A) 닉네임 / email             │
│   역할 [NORMAL] 퍼스널컬러 TRUE_AUTUMN  │
│   선호 컬러 [핑크][블루] (변경)         │
│   [매거진 관리>][상품 관리>] (ADMIN)    │
│   [로그아웃>]                          │
│ 주문: [PAID 2026.06.24]                │
│        키링 ×2  …  24,000원            │
└────────────────────────────────────────┘
```

---

### SCR-07 · CommunityView (커뮤니티)

- **라우트 / 접근권한**: `/community` · 공개 (작성은 로그인 필요)
- **목적**: 자유 게시판과 학생증 자랑(이미지 쇼케이스)을 탭으로 제공하고 글/자랑 작성을 지원한다.
- **관련 FR/UC**: FR-06-01(게시판 목록·탭), FR-06-04(글 작성), FR-06-06(학생증 이미지 업로드), FR-06-05(연관 상품 첨부), FR-03(상품 검색 연동) / UC-06

**주요 구성요소** (`CommunityView.vue` template)
- 헤더 + 액션(SELLER면 "상품 올리기", 로그인 시 "글쓰기"/"학생증 자랑하기"). 게시판 선택은 `?board=showcase` 쿼리.
- 게시판 언더라인 탭 — "자유 게시판" / "🎓 학생증 자랑".
- 작성 폼(`AppCard`, 토글) — 학생증 자랑 모드는 이미지 드롭존(미리보기·제거) + 연관 상품 검색/첨부(`filterProductsByQuery`, 칩으로 선택), 자유 모드는 제목·내용. `canSubmit` 검증.
- 목록 — 학생증 자랑: 3:4 쇼케이스 그리드(이미지·좋아요 배지·작성자·제목). 자유: 행 리스트(제목·발췌·작성자·시간·💬, 세로 좋아요 블록). 비어 있으면 `EmptyState`, 로딩은 `SkeletonBlock`.

**사용자 동작 / 이벤트**
- 탭 전환 → `router.replace`로 쿼리 변경 → `watch`가 `fetchPosts(board)` 재호출.
- 자랑 등록 → 이미지 업로드 후 `createPostAction({title, content, board, imageUrl, productIds})`.
- 좋아요 → `store.toggleLikeAction(postId)`.

**연동 API**
- `GET /api/posts?board={board}` (`store.fetchPosts`)
- `POST /api/posts` (`store.createPostAction`)
- `POST /api/posts/{id}/likes` (`store.toggleLikeAction`)
- `GET /api/products` (연관 상품 후보, `getProducts`)
- `POST /storage/images` (학생증 이미지, `uploadShowcaseImage`)

**와이어프레임**
```text
┌──────────────── /community ────────────────┐
│ Lounge · 커뮤니티               [글쓰기]    │
│ [자유 게시판] [🎓 학생증 자랑]              │
│ (학생증 자랑 그리드)                        │
│ [3:4 ♥5][3:4 ♥9][3:4 ♥3]                    │
│ (A)작성자  제목                             │
│ ── 또는 자유 게시판 행 리스트 ──            │
│ 제목 ………………                  ┌♥┐         │
│ 발췌 …  작성자 · 날짜 · 💬3      │7│         │
└──────────────────────────────────────────────┘
```

---

### SCR-05 · ProductListView (상품 목록)

- **라우트 / 접근권한**: `/products` · 공개
- **목적**: 전체 상품을 카테고리 칩·검색으로 탐색하고, 로그인 회원에게는 For You 추천 섹션을 별도 노출.
- **관련 FR/UC**: FR-03-01(상품 목록·검색·필터), FR-05(For You 추천 노출) / UC-03

**주요 구성요소** (`ProductListView.vue` template)
- 헤더 — 검색 중이면 "'{q}' 검색 결과", 아니면 "꾸미기 아이템".
- For You 추천 섹션(`foryou`) — 로그인 + 추천 보유 + 비검색 시 노출, "{닉네임}님을 위한 추천!", 추천 카드 그리드(`AppBadge` 추천 ✦).
- 카테고리 칩 — "전체" + `PRODUCT_CATEGORIES`(모바일 가로 스크롤).
- 상품 그리드 — `ProductCard`(2→3→4열), `#meta` 슬롯에 상위 3개 컬러 도트. 검색 결과/빈 상태는 `EmptyState`.

**사용자 동작 / 이벤트**
- 헤더 검색(`?q=`) 반영. 카테고리 칩 선택 → `filteredProducts` 필터링(추천 카드와 중복 제외).

**연동 API**
- `GET /api/products` (`store.fetchProducts`)
- `GET /api/recommendations` (로그인 시 For You, `loadRecommendations`)
- `GET /api/users/me` (필요 시 `fetchMe`)

**와이어프레임**
```text
┌──────────── /products ────────────┐
│ Shop · 꾸미기 아이템              │
│ ┌ ○○님을 위한 추천! 추천✦ ─────┐ │
│ │ [rec][rec][rec][rec]         │ │
│ └──────────────────────────────┘ │
│ [전체][핀뱃지][키링][그립톡]…     │
│ [card][card][card][card]         │
│ [card][card][card][card]         │
└────────────────────────────────────┘
```

---

### SCR-04 · OnboardingView (온보딩)

- **라우트 / 접근권한**: `/onboarding` · 로그인(requiresAuth) · hideChatFab
- **목적**: NORMAL 회원이 선호 컬러를 선택해 추천 정확도를 높이고 `onboardingCompleted=true`로 전환(건너뛰기 가능).
- **관련 FR/UC**: FR-01-03(온보딩·선호 컬러 설정) / UC-01

**주요 구성요소** (`OnboardingView.vue` template)
- 헤더("어떤 컬러를 좋아하세요?"), 에러 알럿.
- 컬러 스와치 칩(B3) — `COLOR_OPTIONS`(ALL 제외), 좌측 원형 스와치 + 이름, 다중 선택(`selectedColors`).
- 액션 — "시작하기"(`handleSubmit`), "건너뛰기"(`handleSkip`).

**사용자 동작 / 이벤트**
- 칩 토글 → 선택 누적. "시작하기" → 선택 컬러 + `onboardingCompleted:true` 저장 후 홈. "건너뛰기" → 빈 선호로 완료 처리 후 홈.

**연동 API**
- `GET /api/users/me` (`fetchMe`)
- `PUT /api/users/me` (`{nickname, preferredColors, preferredStyles, onboardingCompleted}`)

**와이어프레임**
```text
┌──────────── /onboarding ────────────┐
│ Welcome                              │
│ 어떤 컬러를 좋아하세요?              │
│ [● 레드][● 핑크][● 블루][● 그린]…    │
│ [   ▌ 시작하기 ▌   ] [ 건너뛰기 ]   │
└──────────────────────────────────────┘
```

---

### SCR-02 · LoginView (로그인)

- **라우트 / 접근권한**: `/login` · 비로그인 전용(guestOnly) · hideChatFab
- **목적**: 이메일/비밀번호로 JWT 로그인. 로그인 후 redirect 또는 온보딩으로 분기.
- **관련 FR/UC**: FR-01-01(로그인) / UC-01

**주요 구성요소** (`LoginView.vue`): 로고, 세그먼트 토글(로그인/회원가입, B5), `AppInput`(이메일·비밀번호), "로그인" `AppButton`, 회원가입 링크. 클라이언트 이메일 형식 검증(`isValidEmail`), 에러 메시지(`authErrorMessage`).

**사용자 동작 / 이벤트**: 제출 → `authStore.loginAction` → `fetchMe` → NORMAL & 미온보딩이면 `/onboarding`, 아니면 `redirect ?? '/'`.

**연동 API**: `POST /api/auth/login`, `GET /api/users/me`.

```text
┌──── /login ────┐  [로그인 | 회원가입]
│ hakku.          │  이메일 [__________]
│ 다시 만나서…    │  비밀번호 [________]
│                 │  [   ▌ 로그인 ▌   ]
└─────────────────┘
```

---

### SCR-03 · SignupView (회원가입)

- **라우트 / 접근권한**: `/signup` · 비로그인 전용(guestOnly) · hideChatFab
- **목적**: NORMAL/SELLER 역할로 회원가입.
- **관련 FR/UC**: FR-01-02(회원가입) / UC-01

**주요 구성요소** (`SignupView.vue`): 세그먼트 토글, `AppInput`(이메일·닉네임·비밀번호·비밀번호 확인, 불일치 경고), 역할 선택 카드(B6: 일반 회원/판매자, 라디오), "회원가입" `AppButton`. `canSubmit` 검증.

**사용자 동작 / 이벤트**: 제출 → `signupAction` → `fetchMe` → SELLER면 `/seller/products`, NORMAL이면 `/onboarding`.

**연동 API**: `POST /api/auth/signup`, `GET /api/users/me`.

```text
┌──── /signup ────┐  [로그인 | 회원가입]
│ 이메일/닉네임/비밀번호/확인          │
│ 가입 유형: [🛍️ 일반 회원][🏪 판매자] │
│ [   ▌ 회원가입 ▌   ]                 │
└──────────────────────────────────────┘
```

---

### SCR-12 · CartView (장바구니)

- **라우트 / 접근권한**: `/cart` · 로그인(requiresAuth)
- **목적**: 담은 상품의 수량 조정·삭제와 주문 요약을 제공하고 주문서로 진행.
- **관련 FR/UC**: FR-03-05(장바구니 CRUD), FR-04-01(주문서 진입) / UC-04

**주요 구성요소** (`CartView.vue`): 항목 목록(썸네일 톤 + 이름·가격 + 수량 스테퍼 ±, 삭제 ×), 주문 요약 카드(상품 금액·배송비·합계, 3만원 이상 무료배송 안내, "주문하기"). 빈/로딩/에러 분기(`SkeletonBlock`). 합계는 `calcCartSummary`.

**사용자 동작 / 이벤트**: 수량 ± → `PUT /cart/items/{id}`. 삭제 → `DELETE`. "주문하기" → `/order/new?amount={total}&count={itemCount}`.

**연동 API**: `GET/PUT/DELETE /api/cart/items` (`apiClient`).

```text
┌──────────── /cart ────────────┐
│ 장바구니                       │
│ [▤] 상품명          [−][2][+] ×│
│ [▤] 상품명          [−][1][+] ×│
│ ┌ 주문 요약 ─────────┐         │
│ │ 상품 금액   24,000원│         │
│ │ 배송비       무료   │         │
│ │ 합계        24,000원│         │
│ │ [   주문하기   ]    │         │
│ └─────────────────────┘         │
└─────────────────────────────────┘
```

---

### SCR-13 · OrderFormView (주문서)

- **라우트 / 접근권한**: `/order/new` · 로그인(requiresAuth) · hideChatFab
- **목적**: 배송지 입력 후 주문(orders)을 생성하고 ORDER 결제로 진행.
- **관련 FR/UC**: FR-04-01(주문 생성·배송지) / UC-04

**주요 구성요소** (`OrderFormView.vue`): 주문 금액 요약, 배송 정보 폼(`AppInput`: 받는 분·연락처·우편번호·주소·상세주소), "자동채움"(데모용), "주문하기". 필수값 검증.

**사용자 동작 / 이벤트**: 제출 → `createOrder(form)` → `/payments/checkout?refType=ORDER&refId={order.id}&amount={totalAmount}&name=주문 N건`.

**연동 API**: `POST /api/orders` (`createOrder`).

```text
┌──── /order/new ────┐
│ ← 장바구니로        │
│ 배송지 입력         │
│ 주문 금액 24,000원  │
│ 배송 정보 [자동채움]│
│ 받는 분 [_______]   │
│ 연락처 [________]   │
│ 우편번호 [______]   │
│ 주소 [__________]   │
│ [   주문하기   ]    │
└─────────────────────┘
```

---

### SCR-15 · PaymentSuccessView (결제 성공)

- **라우트 / 접근권한**: `/payments/success` · 로그인(requiresAuth) · hideChatFab
- **목적**: 토스 successUrl 콜백을 받아 서버 승인(confirm)을 호출해 결제를 최종 확정.
- **관련 FR/UC**: FR-04-03(결제 승인·금액 검증), FR-07(ORDER 알림) / UC-04

**주요 구성요소** (`PaymentSuccessView.vue`): 상태 분기(confirming/approved/failed). approved 시 체크 아이콘 + 금액 + 샌드박스 안내 + "주문 내역 보기"(`/my?tab=orders`) + "쇼핑 계속하기". failed 시 안내 + "장바구니로".

**사용자 동작 / 이벤트**: `onMounted`에서 `paymentKey/orderId/amount` 검증 → `confirmTossPayment` 호출 → `status==='APPROVED'`면 approved.

**연동 API**: `POST /api/payments` (confirm, `confirmTossPayment`, payment-server가 토스 승인 + 금액 위변조 검증).

```text
┌──── /payments/success ────┐
│        (✓)                │
│ 결제가 완료되었어요        │
│        24,000원           │
│ 샌드박스 결제…             │
│ [ 주문 내역 보기 ]         │
│   쇼핑 계속하기            │
└────────────────────────────┘
```

---

### SCR-16 · PaymentFailView (결제 실패)

- **라우트 / 접근권한**: `/payments/fail` · 공개 · hideChatFab
- **목적**: 토스 failUrl 의 실패 사유(code/message)를 표시.
- **관련 FR/UC**: FR-04-03(결제 실패 처리) / UC-04

**주요 구성요소** (`PaymentFailView.vue`): 경고 아이콘, "결제 실패", 메시지(쿼리 `message`), 코드(쿼리 `code`), "장바구니로 돌아가기".

**연동 API**: 없음(쿼리 파라미터만 표시).

```text
┌──── /payments/fail ────┐
│        (!)             │
│ 결제 실패              │
│ 메시지 ……             │
│ 코드: PAY_PROCESS_… │
│ [ 장바구니로 돌아가기 ]│
└─────────────────────────┘
```

---

### SCR-18 · NotificationView (알림함)

- **라우트 / 접근권한**: `/notifications` · 로그인(requiresAuth)
- **목적**: Redis 기반 알림 목록(폴링)을 표시하고 항목 클릭 시 대상 화면으로 라우팅.
- **관련 FR/UC**: FR-07-01(알림 목록 조회), FR-07-02(미읽음 표시·읽음 처리), FR-07-03(딥링크) / UC-07 알림 확인

**주요 구성요소** (`NotificationView.vue`): 헤더, 알림 행(타입 글리프 + 메시지 + 상대시간 + chevron). 타입 아이콘 — COMMENT 💬 / LIKE ❤️ / DIAGNOSIS_COMPLETE 🎨 / FOLLOW 👤 / WISHLIST_LIKE 💖 / ORDER 📦. 클릭 가능 여부(`isClickable`)에 따라 호버/좌측 강조. 빈/로딩/에러 분기.

**사용자 동작 / 이벤트**: 항목 클릭(`handleNotificationClick`) — ORDER→`/my?tab=orders`, DIAGNOSIS_COMPLETE→`/my?view=diagnosis`, FOLLOW→`/users/{actorId}`, WISHLIST_LIKE→`/products/{productId}`, 그 외→`/community/{postId}`. 진입 시 `markAllSeen()`.

**연동 API**: `GET /api/notifications` (폴링, App.vue 10초 주기 + 본 화면 onMounted).

```text
┌──────────── /notifications ────────────┐
│ Notifications · 알림                    │
│ (🎨) 퍼스널컬러 진단이 완료됐어요  방금전>│
│ (💬) ○○님이 댓글을 남겼어요       1시간전>│
│ (📦) 주문이 결제 완료됐어요        1일전 >│
│ (👤) ○○님이 팔로우했어요          2일전 >│
└──────────────────────────────────────────┘
```

---

### SCR-09 · ProfileView (공개 프로필)

- **라우트 / 접근권한**: `/users/:id` · 로그인(requiresAuth)
- **목적**: 다른 사용자의 공개 프로필(팔로우·찜·리뷰·글)을 조회하고 팔로우/찜 좋아요.
- **관련 FR/UC**: FR-01-06(팔로우 토글), FR-03(찜·리뷰 조회), FR-06(글 조회) / UC-06

**주요 구성요소** (`ProfileView.vue`): 프로필 헤더(아바타·닉네임·퍼스널컬러·팔로워/팔로잉 수·선호 컬러 배지·팔로우 버튼 또는 "내 마이페이지"), 언더라인 탭(찜/리뷰/글). 찜 탭은 `WishlistCard`(타인 프로필에서 찜 좋아요 가능). 각 탭 lazy 로드.

**사용자 동작 / 이벤트**: 팔로우 토글(`toggleFollow`, 낙관적). 찜 좋아요(`toggleWishlistLike`, 낙관적). 탭 전환 lazy fetch. 라우트 id 변경 시 상태 초기화 후 재로드.

**연동 API**: `GET /api/users/{id}`(`getPublicProfile`), `POST /api/users/{id}/follow`(`toggleFollow`), `GET /api/users/{id}/wishlist`, `POST /api/wishlist/{id}/likes`, `GET /api/users/{id}/reviews`, `GET /api/posts?author={id}`.

```text
┌──────────── /users/:id ────────────┐
│ ← 뒤로                              │
│ (A) 닉네임  TRUE_AUTUMN             │
│ 12 팔로워  8 팔로잉                 │
│ [핑크][블루]            [ 팔로우 ]  │
│ [찜][리뷰][글]                      │
│ (찜 그리드) [w][w][w][w]            │
└──────────────────────────────────────┘
```

---

### SCR-10 · SellerProductsView (판매자 상품 등록)

- **라우트 / 접근권한**: `/seller/products` · 로그인(requiresAuth, SELLER)
- **목적**: 판매자가 상품을 등록(이름·설명·가격·카테고리·컬러·이미지·구매링크).
- **관련 FR/UC**: FR-03-06(판매자 상품 등록) / UC-03

**주요 구성요소** (`SellerProductsView.vue`): 비SELLER 접근 차단 안내, 성공/에러 메시지, 등록 폼(`AppInput` 상품명, `AppTextarea` 설명, 숫자 가격, `AppSelect` 카테고리, 컬러 다중 선택, 이미지 드롭존 미리보기, 구매 링크), "상품 등록"(이미지 업로드 → 등록). `canSubmit` 검증.

**사용자 동작 / 이벤트**: 제출 → `uploadProductImage` → `createProduct`.

**연동 API**: `POST /storage/images`(이미지), `POST /api/products`(`createProduct`).

```text
┌──── /seller/products ────┐
│ Seller · 상품 등록        │
│ 상품명 [__________]       │
│ 설명 [____________]       │
│ 가격 [______] 카테고리[▾] │
│ 컬러 [레드][핑크][블루]…  │
│ 이미지 [드롭존/파일 선택] │
│ 구매 링크 [https://…]     │
│ [   ▌ 상품 등록 ▌   ]    │
└───────────────────────────┘
```

---

### SCR-20 · MagazineDetailView (매거진 상세)

- **라우트 / 접근권한**: `/magazine/:id` · 공개
- **목적**: 마크다운 매거진 본문을 렌더링하고 본문에 임베드된 상품(`/products/123`)을 카드로 표시.
- **관련 FR/UC**: FR-08-01(매거진 조회), FR-08-02(상품 임베드) / UC-08 매거진 열람

**주요 구성요소** (`MagazineDetailView.vue`): 뒤로가기, 로딩/에러 분기, article(키커·제목·부제·커버 이미지), `MagazineBody`(`content` 마크다운 + `productsById` 임베드). 본문 내 상품 id를 파싱(`parseMagazineBlocks`)해 병렬 조회.

**연동 API**: `GET /api/magazines/{id}`(`getMagazine`), `GET /api/products/{id}`(임베드 상품, 병렬).

```text
┌──────────── /magazine/:id ────────────┐
│ ← 홈으로                               │
│ EDITORIAL                              │
│ 이주의 다꾸 특집                       │
│ 겨울 감성 데코 모음                    │
│ [        커버 이미지        ]          │
│ ## 소제목 … 마크다운 본문 …            │
│ [임베드 상품 카드 →]                   │
└──────────────────────────────────────────┘
```

---

### SCR-21 · AdminMagazineView (매거진 관리)

- **라우트 / 접근권한**: `/admin/magazine` · 로그인(requiresAuth, ADMIN)
- **목적**: 매거진을 발행·수정·정렬(마크다운 본문 + 상품 임베드 + 커버/본문 이미지 업로드).
- **관련 FR/UC**: FR-09-01(매거진 CRUD), FR-08(매거진 발행) / UC-09 관리자 운영

**주요 구성요소** (`AdminMagazineView.vue`): 작성/수정 폼(키커·정렬 순서·제목·부제·커버 이미지 업로더·마크다운 본문 + 본문 사진 추가·발행 토글), 매거진 목록(발행/정렬 관리). 본문에 `/products/123` 한 줄 입력 시 상품 카드 임베드 안내.

**연동 API**: `/api/admin/magazines`(ADMIN CRUD), `POST /storage/images`(커버/본문 이미지).

```text
┌──── /admin/magazine ────┐
│ Admin · 매거진 관리      │
│ 키커[__] 정렬순서[0]     │
│ 제목[________]*          │
│ 부제[________]           │
│ 커버[◍ 이미지 선택]      │
│ 본문(마크다운)[________] │
│  +본문에 사진 추가       │
│ □ 메인에 노출(발행)      │
│ [ 발행 / 수정 저장 ]     │
│ ── 매거진 목록 ──        │
└──────────────────────────┘
```

---

### SCR-22 · AdminProductsView (상품 전체 관리)

- **라우트 / 접근권한**: `/admin/products` · 로그인(requiresAuth, ADMIN)
- **목적**: 전체 상품을 한 페이지에서 인라인 편집(활성화·이름·카테고리·스타일 태그), 커서 기반 무한 스크롤.
- **관련 FR/UC**: FR-09-02(상품 전체 관리·인라인 수정) / UC-09

**주요 구성요소** (`AdminProductsView.vue`): 상품 행 리스트(썸네일 + 이름 입력 + 카테고리 select + 태그 입력 + 활성화 토글 + "저장"/"저장됨 ✓"), 무한 스크롤 sentinel(`IntersectionObserver`, 커서 페이징 PAGE_SIZE=20), 로딩/마지막/빈 상태.

**사용자 동작 / 이벤트**: 행 편집 후 "저장" → `editAdminProduct(row)`. 스크롤 하단 도달 시 `listAdminProducts(cursor)` 추가 로드.

**연동 API**: `GET /api/admin/products?cursor=`(`listAdminProducts`), `PUT /api/admin/products/{id}`(`editAdminProduct`).

```text
┌──────────── /admin/products ────────────┐
│ ADMIN · 상품 관리 / 인라인 편집          │
│ [▤] 이름[____] 카테고리[▾] 태그[____]    │
│       [● 활성화] [저장] 저장됨 ✓          │
│ [▤] 이름[____] 카테고리[▾] 태그[____]    │
│       [○ 활성화] [저장]                   │
│  … (스크롤 시 커서로 더 불러옴) …         │
│  — 마지막 상품입니다 —                    │
└────────────────────────────────────────────┘
```

---

## 4. 공통 UI

전역 레이아웃은 `App.vue`가 `MarqueeStrip → AppHeader → router-view(페이지 트랜지션) → AppFooter → AppBottomNav → ChatFab` 순으로 구성한다.

### 4.1 전역 내비게이션

**AppHeader (`components/layout/AppHeader.vue`)** — sticky 상단 헤더(반투명 + blur).
- 로고(워드마크 이미지) → 홈.
- 데스크톱 언더라인 내비(`navItems`): 홈·상품·커뮤니티 + 로그인 시 진단·추천 + SELLER 시 판매.
- 우측 액션: 검색 알약(≥1024, 제출 시 `/products?q=`), 인증 시 퍼스널컬러 배지(`pc-badge`, 진단 완료 시 accent 로 물듦 → `/my`), 알림 아이콘(`notificationStore.hasUnread` 미읽음 점 배지), 마이페이지, 장바구니, 로그아웃(데스크톱). 비인증 시 로그인/회원가입.

**AppBottomNav (`components/layout/AppBottomNav.vue`)** — 모바일 전용 하단 고정 탭바(`md:hidden`).
- 비인증: 홈·상품·커뮤니티·로그인.
- 인증: 홈·상품·커뮤니티·진단·마이. 활성 탭은 accent(진단 후 퍼스널컬러)로 물듦.

> 헤더의 퍼스널컬러 배지/하단탭 활성색은 `App.vue`의 `applyPersonalColorTheme(personalColor)` 감시로 진단 결과에 따라 전역 액센트가 동적으로 주입된다(사이트 전체가 내 컬러로 물듦).

### 4.2 학꾸AI 챗봇 FAB (ChatFab) — SSE

**ChatFab (`components/chat/ChatFab.vue`)** — 우측 하단 고정 FAB("✦ AI 도우미").
- `route.meta.hideChatFab === true` 인 화면(로그인·회원가입·온보딩·주문서·결제 checkout/success/fail)에서는 숨김(`isHidden`).
- 최초 방문 시 1회 안내 말풍선("학생증 꾸미기 고민? 학꾸 AI에게 물어보세요!"), `localStorage` 키 `hakku:chatFabHintDismissed`로 재노출 방지.
- 클릭 시 `ChatWindow`(별도 컴포넌트) 오픈. 챗봇은 SSE 스트리밍(`/chat/stream`) + Redis 1시간 대화기억 + function-calling 도구(get_order_history·get_wishlist·recommend_products, 최대 3라운드) + 상품 카드 임베딩(팩트시트 §7).
- 모션: `hk-pulse` 펄스 애니메이션(reduced-motion 시 비활성), 호버 scale.

### 4.3 알림 표시 및 폴링

- `App.vue`가 인증 상태에서 10초 주기로 `notificationStore.fetchNotifications()` 폴링. 진단이 PENDING이면 동시에 `fetchMe()`로 상태 동기화.
- 헤더 알림 아이콘에 미읽음 점 배지(`hasUnread`). `/notifications` 진입 또는 다른 보호경로 이동 시 갱신, `/notifications`에서는 `markAllSeen()`.
- 알림 파이프라인: main 생산 → Kafka `notification.created` → Redis List(`user:{id}:notifications`, 최대 50) → 프런트 폴링(팩트시트 §7).

### 4.4 공통 UI 컴포넌트 (요약)

| 컴포넌트 | 용도 | 사용 화면(예) |
|---|---|---|
| `ProductCard` | 상품 카드(이미지/플레이스홀더 + meta 슬롯) | SCR-01, 05, 06, 17 |
| `SectionHeader` | 섹션 eyebrow/제목/액션 | SCR-01, 17 |
| `AppCard` / `AppModal` | 카드 컨테이너 / 모달 | 전반 |
| `AppButton` / `AppInput` / `AppTextarea` / `AppSelect` / `AppBadge` | 폼·버튼·배지 | 전반 |
| `SkeletonBlock` / `SkeletonList` / `EmptyState` | 로딩 스켈레톤 / 빈 상태 | 전반 |
| `StarRating` / `ReviewItem` | 평점 / 리뷰 항목 | SCR-06, 09, 19 |
| `WishlistCard` / `UserListItem` | 찜 카드 / 사용자 항목 | SCR-09, 19 |
| `HeroCarousel` | 홈 히어로(진단 상태별) | SCR-01 |
| `MagazineBody` | 마크다운 + 상품 임베드 렌더 | SCR-20 |
| `useAuthedImage` | JWT 첨부 storage 이미지 fetch | SCR-01, 11, 19 |

> 상품 이미지가 없을 때는 상품 id 기반 웜 톤 그라데이션 8종(`u-tone-{id % 8}`)으로 안정적 플레이스홀더를 표시하며, 이는 `ProductCard`·상품 상세·장바구니·커뮤니티에서 동일 규칙을 따른다.
