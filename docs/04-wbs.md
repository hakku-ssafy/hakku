# 학꾸(Hakku) WBS — 작업분해구조 (Work Breakdown Structure)

프로젝트: 학꾸(Hakku) · 작성일: 2026-06-25 · 버전 1.0

---

## 1. 개요

### 1.1 목적

본 문서는 학꾸(Hakku) 프로젝트의 전체 작업 범위를 누락 없이(MECE) 식별하고, 이를 관리 가능한 작업 패키지(Work Package)와 하위 작업으로 분해하여 담당자·산출물·일정 단계에 매핑하는 것을 목적으로 한다. WBS는 일정 관리(`03-gantt`), 요구사항 추적(FR/NFR), 화면 설계(SCR), 데이터 모델(테이블)과 상호 참조되며, 2인 팀의 책임 경계를 명확히 하여 병렬 개발과 통합 검증의 기준선을 제공한다.

### 1.2 분해 기준

학꾸는 Nginx 리버스 프록시(`:19001`) 뒤에 6개 서비스가 배치된 폴리글랏 마이크로서비스이다. 본 WBS는 다음 원칙에 따라 분해한다.

| 분해 기준 | 설명 |
|---|---|
| 기능·도메인 단위 | 회원·인증, AI 퍼스널컬러, 커머스·결제, 커뮤니티·콘텐츠 등 비즈니스 도메인을 1차 분류 축으로 사용한다. |
| 모듈·서비스 단위 | 각 도메인 하위에서 서비스(main-server / payment-server / ai-server / chatbot-server / storage-server / frontend) 경계를 고려하여 리프(leaf) 작업으로 분해한다. |
| 산출물 중심 | 각 작업은 명확한 결과물(코드·스키마·테스트·문서·인프라 구성)을 가지도록 정의한다. |
| 단계 정합성 | 모든 작업은 일정 단계(P1~P5, 팩트시트 §9)와 정렬한다. |

대분류는 W1~W7의 7개 작업 패키지로 구성하며, 각 패키지는 다이어그램(`wbs.mmd`)의 리프 노드와 동일한 집합(총 31개 리프)을 갖는다.

### 1.3 팀 구성 (팩트시트 §8)

| 구분 | 팀원 | 역할 라벨 | 책임 범위 |
|---|---|---|---|
| 팀장 | 천창현 (rearleg) | Lead / BE · AI · Infra · FE | Main Server 전 도메인(회원·인증·커뮤니티·상품·주문·추천·알림·매거진), AI Server(퍼스널컬러 진단 파이프라인), Storage Server(Go + Spring 대조군·벤치마크), Frontend 대부분, 관측성(Prometheus/Grafana/Jaeger)·k6 부하테스트, 데이터셋 |
| 팀원 | 김해찬 (gocks1180) | 결제 · 챗봇 · 협업환경 | Payment Server(토스페이먼츠 결제·Outbox·웹훅·Flyway·운영 릴레이), Chatbot Server(SSE 챗봇 + FAB UI), 결제 프론트(체크아웃·성공·실패), GitHub 협업 템플릿(이슈/PR), 인프라 결선(docker-compose·nginx·prometheus·env) |

기획·요구사항 정의, 설계 산출물 작성, 통합 테스트, 버그픽스·발표 준비 등 프로젝트 공통 작업은 **공통**으로 표기한다.

---

## 2. WBS 다이어그램

아래 다이어그램은 학꾸 프로젝트의 전체 작업 트리(루트 → W1~W7 → 리프 작업)를 나타낸다.

```d2
direction: down
root: "학꾸(Hakku) 설계 · 개발" { style.fill: "#211C16"; style.font-color: "#ffffff"; style.bold: true; style.font-size: 20 }
W1: "W1 기획·관리" {
  style.fill: "#FBF3DC"
  a: "요구사항 정의"
  b: "WBS·일정"
  c: "협업환경(GitHub 템플릿)"
  d: "설계 산출물"
  a -> b -> c -> d { style.opacity: 0 }
}
W2: "W2 공통·인프라" {
  style.fill: "#FBF3DC"
  a: "아키텍처 설계"
  b: "Docker Compose·Nginx"
  c: "PostgreSQL·Flyway"
  d: "Redis·Kafka"
  e: "관측성(Prom·Grafana·Jaeger)"
  a -> b -> c -> d -> e { style.opacity: 0 }
}
W3: "W3 회원·인증" {
  style.fill: "#FBF3DC"
  a: "회원가입"
  b: "로그인·JWT"
  c: "온보딩"
  d: "프로필·팔로우"
  a -> b -> c -> d { style.opacity: 0 }
}
W4: "W4 AI 퍼스널컬러" {
  style.fill: "#FBF3DC"
  a: "ai-server·전처리"
  b: "이미지 생성(gpt-image-2)"
  c: "세부타입 추출·16종 ENUM"
  d: "Storage 연동·상태머신"
  a -> b -> c -> d { style.opacity: 0 }
}
W5: "W5 커머스·결제" {
  style.fill: "#FBF3DC"
  a: "상품 CRUD·검색"
  b: "장바구니·찜·리뷰"
  c: "주문"
  d: "결제(토스·Outbox·웹훅)"
  e: "추천 엔진"
  a -> b -> c -> d -> e { style.opacity: 0 }
}
W6: "W6 커뮤니티·콘텐츠" {
  style.fill: "#FBF3DC"
  a: "게시글·댓글·좋아요"
  b: "학생증 자랑"
  c: "매거진"
  d: "알림(Kafka→Redis)"
  e: "AI 챗봇(SSE)"
  a -> b -> c -> d -> e { style.opacity: 0 }
}
W7: "W7 품질·검증" {
  style.fill: "#FBF3DC"
  a: "단위·통합 테스트"
  b: "k6 부하 테스트"
  c: "Go vs Spring 벤치마크"
  d: "버그픽스·발표"
  a -> b -> c -> d { style.opacity: 0 }
}
root -> W1
root -> W2
root -> W3
root -> W4
root -> W5
root -> W6
root -> W7
```

> 그림 4-1. 학꾸 WBS

---

## 3. WBS 상세표

대분류 W1~W7과 각 하위 작업의 코드, 작업명, 주요 산출물(관련 FR/NFR/SCR/테이블 상호참조 포함), 담당자, 관련 단계(P1~P5)를 정리한다. 리프 작업의 명칭은 그림 4-1의 노드와 일치한다.

### W1 — 기획·관리

| WBS코드 | 작업명 | 산출물 / 결과물 | 담당자 | 관련 단계 |
|---|---|---|---|---|
| W1 | 기획·관리 | 프로젝트 전반의 기획·문서·협업 기반 | 공통 | P1, P5 |
| W1.1 | 요구사항 정의 | 기능 요구사항(FR-01~FR-10 영역), 비기능 요구사항(NFR), 액터 정의(NORMAL/SELLER/ADMIN/Guest) | 공통 | P1 |
| W1.2 | WBS·일정 | 본 WBS 문서(W1~W7), 마일스톤(M1~M5), 간트 차트 | 공통 | P1 |
| W1.3 | 협업환경(GitHub 템플릿) | 이슈 템플릿, PR 템플릿, 브랜치 전략, GitHub 협업 규약 | 김해찬 | P3 |
| W1.4 | 설계 산출물 | PRD·아키텍처·ERD·화면설계·기술스택 선정서, 통합 설계 문서(docs) | 공통 | P1, P5 |

### W2 — 공통·인프라

| WBS코드 | 작업명 | 산출물 / 결과물 | 담당자 | 관련 단계 |
|---|---|---|---|---|
| W2 | 공통·인프라 | 6개 서비스 실행 기반과 지원 인프라 | 천창현 / 김해찬 | P1~P3 |
| W2.1 | 아키텍처 설계 | 마이크로서비스 경계 정의(NFR 확장성), 서비스 간 통신·API 표면 설계(§6) | 천창현 | P1 |
| W2.2 | Docker Compose·Nginx | docker-compose, Nginx 리버스 프록시(`:19001`) 라우팅, env 구성, 인프라 결선 (NFR 가용성) | 김해찬 | P1~P2 |
| W2.3 | PostgreSQL·Flyway | hakku DB 스키마(테이블 20종), main Flyway V1~V19, payment Flyway V1~V2(이력 분리) | 천창현 | P2~P3 |
| W2.4 | Redis·Kafka | Redis 7(알림 List·챗봇 history·JWT 블랙리스트·캐시), Kafka 3.7(KRaft) 토픽(notification.created, payment.approved/failed) | 천창현 | P2~P3 |
| W2.5 | 관측성(Prom·Grafana·Jaeger) | Prometheus 메트릭, Grafana 대시보드(`:3000`), Jaeger 분산추적(`:16686`) (NFR 관측성) | 천창현 | P3 |

### W3 — 회원·인증

| WBS코드 | 작업명 | 산출물 / 결과물 | 담당자 | 관련 단계 |
|---|---|---|---|---|
| W3 | 회원·인증 | 회원 가입·인증·프로필·소셜 그래프 (FR-01) | 천창현 | P2~P3 |
| W3.1 | 회원가입 | 회원가입 API(POST /api/auth/signup), users 테이블, SignupView(SCR-03) | 천창현 | P2 |
| W3.2 | 로그인·JWT | 로그인/로그아웃/리프레시(JWT), jwt:blacklist Redis, LoginView(SCR-02), 라우트 가드(requiresAuth/guestOnly) (NFR 보안) | 천창현 | P2 |
| W3.3 | 온보딩 | 선호 컬러·스타일 설정(user_preferred_colors/styles), onboarding_completed 게이트, OnboardingView(SCR-04) | 천창현 | P2 |
| W3.4 | 프로필·팔로우 | follows 테이블, 팔로우/팔로워 API, ProfileView(SCR-09) | 천창현 | P3 |

### W4 — AI 퍼스널컬러

| WBS코드 | 작업명 | 산출물 / 결과물 | 담당자 | 관련 단계 |
|---|---|---|---|---|
| W4 | AI 퍼스널컬러 | AI 진단 파이프라인 전체 (FR-02, UC 진단 흐름) | 천창현 | P3 |
| W4.1 | ai-server·전처리 | FastAPI ai-server, JWT 검증·202 즉시반환, 이미지 resize 전처리, DiagnosisView(SCR-11) | 천창현 | P3 |
| W4.2 | 이미지 생성(gpt-image-2) | OpenAI gpt-image-2 연동, 진단용 이미지 생성, diagnosis_image_url | 천창현 | P3 |
| W4.3 | 세부타입 추출·16종 ENUM | gpt-5.4-mini 텍스트 추출, PersonalColorType 16종 ENUM 파싱, users.personal_color PATCH | 천창현 | P3 |
| W4.4 | Storage 연동·상태머신 | storage-server result 저장(JWT ownerId 접근제어), DiagnosisStatus 상태머신(NONE→PENDING→COMPLETED, 실패 시→NONE), Kafka DIAGNOSIS_COMPLETE | 천창현 | P3 |

### W5 — 커머스·결제

| WBS코드 | 작업명 | 산출물 / 결과물 | 담당자 | 관련 단계 |
|---|---|---|---|---|
| W5 | 커머스·결제 | 상품·장바구니·주문·결제·추천 (FR-03/04/05) | 천창현 / 김해찬 | P2~P4 |
| W5.1 | 상품 CRUD·검색 | products·product_styles/colors, SELLER 등록·수정·삭제, 목록·검색·필터, ProductListView(SCR-05)·ProductDetailView(SCR-06)·SellerProductsView(SCR-10) (FR-03) | 천창현 | P2 |
| W5.2 | 장바구니·찜·리뷰 | cart_items·wishlists·wishlist_likes·reviews, CartView(SCR-12) (FR-03) | 천창현 | P2~P3 |
| W5.3 | 주문 | orders·order_items(스냅샷), 주문서 작성·배송지, OrderFormView(SCR-13), OrderStatus(CREATED→PAID→CANCELLED) (FR-04) | 천창현 | P4 |
| W5.4 | 결제(토스·Outbox·웹훅) | payment-server, 토스페이먼츠 Intent→Charge→Settle, payments(idempotency_key UK·@Version)·payment_outbox, 웹훅(HMAC-SHA256), OutboxRelay→Kafka, PaymentCheckout/Success/Fail(SCR-14~16) (FR-04, NFR 보안) | 김해찬 | P4 |
| W5.5 | 추천 엔진 | RecommendationScoreCalculator(퍼스널컬러·선호·행동로그·인기도), 설명가능 점수 분해, RecommendationView(SCR-17) (FR-05) | 천창현 | P3 |

### W6 — 커뮤니티·콘텐츠

| WBS코드 | 작업명 | 산출물 / 결과물 | 담당자 | 관련 단계 |
|---|---|---|---|---|
| W6 | 커뮤니티·콘텐츠 | 커뮤니티·매거진·알림·챗봇 (FR-06/07/08/10) | 천창현 / 김해찬 | P2~P4 |
| W6.1 | 게시글·댓글·좋아요 | posts·comments·post_likes, 자유게시판, CommunityView(SCR-07)·PostDetailView(SCR-08) (FR-06) | 천창현 | P2 |
| W6.2 | 학생증 자랑 | board=STUDENT_ID, post_products(연관상품), 학생증 자랑 탭·그리드 (FR-06) | 천창현 | P2~P3 |
| W6.3 | 매거진 | magazines(마크다운), 발행 조회·상품 임베드, MagazineDetailView(SCR-20), AdminMagazineView(SCR-21)·AdminProductsView(SCR-22) (FR-08, FR-09) | 천창현 | P4 |
| W6.4 | 알림(Kafka→Redis) | NotificationConsumer, Redis List(user:{id}:notifications 최대 50), NotificationType(COMMENT/LIKE/FOLLOW 등), NotificationView(SCR-18 폴링) (FR-07) | 천창현 | P3~P4 |
| W6.5 | AI 챗봇(SSE) | chatbot-server, SSE 스트리밍, Redis 1시간 대화기억, function-calling(get_order_history/get_wishlist/recommend_products), 전역 ChatFab (FR-10) | 김해찬 | P2~P3 |

### W7 — 품질·검증

| WBS코드 | 작업명 | 산출물 / 결과물 | 담당자 | 관련 단계 |
|---|---|---|---|---|
| W7 | 품질·검증 | 테스트·성능·벤치마크·마무리 (NFR 성능·가용성) | 공통 / 천창현 | P5 |
| W7.1 | 단위·통합 테스트 | 도메인 단위 테스트, API 통합 테스트, 결제 멱등성·Outbox 검증 | 공통 | P5 |
| W7.2 | k6 부하 테스트 | k6 시나리오, 처리량·지연 측정 리포트 (NFR 성능) | 천창현 | P5 |
| W7.3 | Go vs Spring 벤치마크 | storage-server(Go) vs storage-server-spring 대조 벤치마크 결과 | 천창현 | P5 |
| W7.4 | 버그픽스·발표 | 통합 버그픽스, 최종 설계 문서, 발표 자료·데모 (M5) | 공통 | P5 |

---

## 4. 작업 패키지 설명

### W1 기획·관리
프로젝트의 방향성과 협업 기반을 확립하는 패키지이다. 요구사항(FR/NFR)과 액터를 확정하고, 본 WBS·일정·마일스톤을 수립하며, 설계 산출물(PRD·아키텍처·ERD·화면설계)을 작성한다. GitHub 이슈·PR 템플릿 등 협업환경 구축은 김해찬이 담당하고, 그 외 기획·문서는 공통으로 수행한다.

### W2 공통·인프라
6개 마이크로서비스가 동작하는 실행 기반과 지원 인프라를 구성한다. 천창현이 아키텍처 설계와 PostgreSQL·Flyway, Redis·Kafka, 관측성(Prometheus/Grafana/Jaeger)을 주도한다. docker-compose·Nginx 리버스 프록시·env 등 인프라 결선은 김해찬이 담당하여 서비스 간 라우팅과 배포 환경을 확립한다.

### W3 회원·인증
회원 가입부터 JWT 기반 인증, 온보딩, 프로필·팔로우까지 사용자 계정 도메인을 구축한다. main-server에 회원·인증 로직과 라우트 가드, Redis JWT 블랙리스트를 포함하며 천창현이 단독 담당한다. NORMAL 회원의 온보딩 게이트가 이후 추천·진단 기능의 전제가 된다.

### W4 AI 퍼스널컬러
얼굴 사진을 입력받아 16종 퍼스널컬러를 진단하는 AI 파이프라인 전체이다. FastAPI ai-server가 JWT 검증 후 202를 즉시 반환하고, 백그라운드에서 gpt-image-2 이미지 생성과 gpt-5.4-mini 세부타입 추출을 수행하여 storage 저장·상태머신 전이·Kafka 이벤트까지 처리한다. 진단 파이프라인 전반을 천창현이 담당한다.

### W5 커머스·결제
상품 CRUD·검색, 장바구니·찜·리뷰, 주문, 결제, 추천 엔진을 포함하는 커머스 핵심 패키지이다. 상품·장바구니·주문·추천은 main-server에서 천창현이 구현하며, 토스페이먼츠 결제는 별도 payment-server에서 Intent→Charge→Settle 흐름과 트랜잭셔널 Outbox·웹훅·운영 릴레이로 김해찬이 구현한다. 추천 엔진은 설명가능한 점수 분해를 제공한다.

### W6 커뮤니티·콘텐츠
자유게시판·학생증 자랑 커뮤니티, 마크다운 매거진, Kafka→Redis 알림 파이프라인, AI 고객센터 챗봇을 포함한다. 게시글·매거진·알림은 천창현이 main-server에서 담당하고, SSE 스트리밍·function-calling·1시간 대화기억을 갖춘 chatbot-server와 전역 ChatFab은 김해찬이 담당한다.

### W7 품질·검증
프로젝트 마무리 단계의 품질 보증 패키지이다. 단위·통합 테스트와 버그픽스·발표 준비는 공통으로 수행하며, k6 부하 테스트와 Go vs Spring(storage-server) 성능 벤치마크는 관측성·성능을 총괄하는 천창현이 담당한다. 최종 산출물 정리와 데모로 마일스톤 M5를 달성한다.

---

## 5. 역할과 책임(R&R)

팀원별 담당 작업 패키지·모듈과 책임을 정리한다. 담당 배정은 팩트시트 §8의 책임 범위를 근거로 한다.

| 팀원 | 역할 | 담당 WBS(리프) | 담당 모듈·서비스 | 핵심 책임 |
|---|---|---|---|---|
| 천창현 | Lead / BE · AI · Infra · FE | W2.1, W2.3, W2.4, W2.5, W3.1~W3.4, W4.1~W4.4, W5.1, W5.2, W5.3, W5.5, W6.1, W6.2, W6.3, W6.4, W7.2, W7.3 | main-server(전 도메인), ai-server, storage-server(Go+Spring), frontend 대부분, 관측성 스택 | 아키텍처 총괄, 회원·인증·커뮤니티·상품·주문·추천·알림·매거진, AI 진단 파이프라인, 성능·벤치마크 |
| 김해찬 | 결제 · 챗봇 · 협업환경 | W1.3, W2.2, W5.4, W6.5 | payment-server, chatbot-server, 결제 프론트(SCR-14~16), Nginx·docker-compose 결선, GitHub 템플릿 | 토스페이먼츠 결제·Outbox·웹훅, SSE 챗봇·FAB, 인프라 결선, 협업환경 구축 |
| 공통 | — | W1.1, W1.2, W1.4, W7.1, W7.4 | 기획·문서·테스트 전반 | 요구사항·WBS·설계 산출물, 단위·통합 테스트, 버그픽스·발표 준비 |

### 5.1 책임 분담 원칙

| 항목 | 내용 |
|---|---|
| 단일 책임자(DRI) | 각 리프 작업은 1인의 주담당을 갖되, 공통 작업은 양 팀원이 공동 책임을 진다. |
| 서비스 경계 정합성 | payment-server·chatbot-server는 김해찬, 그 외 main/ai/storage 서비스는 천창현이 담당하여 마이크로서비스 경계와 책임 경계를 일치시킨다. |
| 인터페이스 합의 | 결제(payment.approved/failed)·알림(notification.created) 등 Kafka 토픽과 크로스모듈 논리 참조(payments.user_id/reference_id)는 양 팀원이 인터페이스를 합의한다. |
| 통합·검증 공동 책임 | W7.1·W7.4는 공통으로, 서비스 간 통합 지점(결제→주문 전이, 진단→알림)의 회귀를 함께 검증한다. |
