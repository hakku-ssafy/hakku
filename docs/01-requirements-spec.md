# 요구사항 정의서 (Software Requirements Specification)

프로젝트: 학꾸(Hakku) · 작성일: 2026-06-25 · 버전 1.0

---

## 1. 문서 개요

### 1.1 목적

본 문서는 AI 퍼스널컬러 기반 꾸미기 아이템 추천 커머스·커뮤니티 플랫폼인 **학꾸(Hakku)**의 소프트웨어 요구사항을 정의한다. 본 문서는 기능 요구사항(FR)과 비기능 요구사항(NFR)을 명세하고, 각 요구사항을 화면(SCR) 및 유스케이스(UC)와 상호 추적 가능하도록 매핑한다. 본 문서는 설계·구현·검증 전 단계의 기준선(baseline) 역할을 하며, 이후 산출물(아키텍처 설계서·화면 설계서·시스템 설계서·테스트 계획서 등)은 본 문서의 요구사항 ID를 참조한다.

### 1.2 범위

학꾸는 다음 기능을 단일 폴리글랏 마이크로서비스 스택으로 제공한다.

- **회원·인증**: 회원가입(일반회원·판매자), JWT 로그인/로그아웃/토큰 재발급, 온보딩(선호 컬러·스타일 설정), 프로필·팔로우
- **AI 퍼스널컬러 진단**: 얼굴 사진 업로드 → 16종 퍼스널컬러 진단 → 진단 결과 반영
- **상품·커머스**: 상품 등록/조회/검색, 장바구니, 찜(위시리스트), 리뷰
- **주문·결제**: 주문서 작성, 토스페이먼츠 결제 승인·웹훅·Outbox 이벤트
- **개인화 추천**: 퍼스널컬러·선호도·행동 이력 기반 설명 가능한 추천
- **커뮤니티**: 자유게시판, 학생증 자랑 게시판, 댓글·좋아요·연관 상품
- **알림**: Kafka 기반 비동기 알림 파이프라인, 알림함 폴링
- **매거진**: 마크다운 매거진 조회·상품 임베드
- **관리자**: 매거진 CRUD, 상품 전체 관리
- **AI 챗봇**: SSE 스트리밍 고객센터 챗봇(function-calling, 대화 기억)

범위에서 제외되는 사항은 다음과 같다. 실제 물류·배송 추적(주문은 결제 완료까지만 처리), 정산·세금계산서 발행, 외부 PG의 토스페이먼츠 외 결제수단 확장, 모바일 네이티브 앱(웹 반응형으로 대응).

### 1.3 대상 독자

| 독자 | 활용 목적 |
|---|---|
| 프로젝트 평가위원(SSAFY) | 요구사항 완전성·일관성·추적성 검토 |
| 개발자(천창현·김해찬) | 구현 범위·우선순위·인수 기준 확인 |
| 설계 문서 작성자 | 아키텍처/화면/시스템 설계의 입력으로 활용 |
| 테스터 | 테스트 케이스 도출 및 추적 매트릭스 기반 검증 |

### 1.4 용어 및 약어 정의

| 용어/약어 | 정의 |
|---|---|
| MSA | Microservice Architecture, 마이크로서비스 아키텍처 |
| 폴리글랏(Polyglot) | 서비스별로 상이한 언어·런타임을 채택한 구성(Java·Python·Go) |
| 퍼스널컬러 | 개인의 피부·눈·머리카락 톤에 어울리는 색 계열. 본 서비스는 4계절×4톤 = 16종으로 분류 |
| JWT | JSON Web Token, 무상태 인증 토큰 |
| SSE | Server-Sent Events, 서버→클라이언트 단방향 스트리밍 |
| Outbox | 트랜잭셔널 아웃박스 패턴. DB 트랜잭션과 메시지 발행의 원자성을 보장 |
| 멱등성(Idempotency) | 동일 요청을 여러 번 보내도 결과가 한 번 처리한 것과 동일한 성질 |
| HMAC-SHA256 | 키 기반 메시지 인증 코드. 웹훅 위·변조 검증에 사용 |
| at-least-once | 메시지가 최소 한 번 이상 전달됨을 보장하는 전달 의미 |
| 온보딩 게이트 | 온보딩 미완료 일반회원이 보호 경로 접근 시 온보딩으로 강제 이동시키는 가드 |
| FR / NFR | Functional / Non-Functional Requirement |
| UC | Use Case, 유스케이스 |
| SCR | Screen, 화면 식별자 |
| KRaft | Kafka 자체 합의 모드(ZooKeeper 미사용) |
| p95 / p99 | 95/99 백분위 지연 시간(latency percentile) |
| VU | Virtual User, k6 부하 테스트의 가상 사용자 |

### 1.5 참고 문서

| 문서 | 비고 |
|---|---|
| 학꾸 통합 팩트시트 (CANONICAL SOURCE OF TRUTH) | 엔티티·ENUM·화면·요구사항 ID의 진실의 원천 |
| 프로젝트 README (`README.md`) | 아키텍처·벤치마크 결과·실행 방법 |
| Flyway 마이그레이션 V1~V19, payment V1~V2 | 최종 누적 데이터 스키마 |
| 02 아키텍처 설계서 / 03 화면 설계서 / 04 시스템 설계서 | 본 문서를 입력으로 하는 후속 산출물 |

---

## 2. 시스템 개요

### 2.1 서비스 정의

학꾸는 사용자가 얼굴 사진을 업로드하면 AI가 16종 퍼스널컬러를 진단하고, 진단 결과와 활동 이력을 바탕으로 꾸미기 아이템을 개인화 추천하는 **커머스·커뮤니티 플랫폼**이다. 토스페이먼츠 결제, AI 고객센터 챗봇, 마크다운 매거진, 학생증 자랑 커뮤니티를 단일 폴리글랏 마이크로서비스 스택으로 통합한다.

### 2.2 사용자 유형(액터)

#### 2.2.1 인간 액터

| 액터 | 권한 코드 | 권한 범위 |
|---|---|---|
| 비회원 | (Guest) | 홈·상품·커뮤니티·매거진 조회 |
| 일반회원 | NORMAL | 비회원 권한 + 장바구니·주문·결제·리뷰·찜·팔로우·커뮤니티 작성·AI 진단·추천·알림 (온보딩 필수) |
| 판매자 | SELLER | 일반회원 권한 + 본인 상품 등록/수정/삭제 |
| 관리자 | ADMIN | 매거진 CRUD·상품 전체 관리 |

권한 일반화 관계: **판매자(SELLER) ▷ 일반회원(NORMAL) ▷ 조회는 비회원 공통**. 즉 상위 권한은 하위 권한의 모든 기능을 포함한다.

#### 2.2.2 외부/시스템 액터

| 액터 | 역할 |
|---|---|
| AI Server | 퍼스널컬러 진단 파이프라인 수행, OpenAI 연동 |
| OpenAI | gpt-image-2(이미지 생성)·gpt-5.4-mini(텍스트 추출) 모델 제공 |
| 토스페이먼츠(PG) | 결제 승인·웹훅 발송 |
| Storage Server | 이미지 저장·서빙 |
| Kafka | 서비스 간 비동기 이벤트 브로커 |
| 학꾸AI 챗봇 | 고객센터 SSE 챗봇(function-calling) |

### 2.3 폴리글랏 MSA 아키텍처

학꾸는 Nginx 리버스 프록시(host 포트 `:19001`) 뒤에 6개 애플리케이션 서비스가 독립 실행되는 폴리글랏 마이크로서비스 구조다. 비즈니스 로직은 Spring(`main-server`·`payment-server`), AI 추론은 FastAPI(`ai-server`·`chatbot-server`), 이미지 입출력은 Go(`storage-server`)가 각각 최적 런타임으로 담당한다.

| 서비스 | 스택 | 책임 | 라우팅 경로 |
|---|---|---|---|
| frontend | Vue 3 + Vite + TypeScript + Tailwind + Pinia | 사용자 화면(22개) | `/` |
| main-server | Spring Boot 4.0.6 (Java 17), Spring Data JDBC + Flyway | 회원·인증·커뮤니티·매거진·상품·주문·추천·알림 | `/api/` |
| payment-server | Spring Boot 4.0.6 (Java 17), JPA + Flyway | 토스페이먼츠 결제 승인·웹훅·Outbox 이벤트 | `/api/payments` |
| ai-server | FastAPI (Python 3.13) | 퍼스널컬러 진단, OpenAI 연동 | `/ai/` |
| chatbot-server | FastAPI + OpenAI | AI 고객센터 챗봇(SSE·function-calling) | `/chat/` |
| storage-server | Go 1.24 (표준 라이브러리) | 이미지 저장·서빙 (대조군 storage-server-spring) | `/storage/` |

지원 인프라는 PostgreSQL 16, Redis 7, Kafka 3.7(KRaft 모드)이며, 관측성 스택으로 Prometheus, Grafana(:3000), Jaeger(:16686)를 운용한다. 외부 연동은 OpenAI(gpt-image-2, gpt-5.4-mini)와 토스페이먼츠다. `payment-server`는 `main-server`와 **동일한 hakku PostgreSQL DB를 공유**하되 Flyway 이력 테이블(`flyway_schema_history_payment`, baseline 0)을 분리해 마이그레이션 충돌을 방지한다.

#### 2.3.1 아키텍처 구성도

```d2
direction: down
client: "클라이언트 — Vue 3 SPA / 브라우저" { shape: person }
nginx: "Nginx 리버스 프록시 :19001" { shape: hexagon }
services: "애플리케이션 서비스 (6)" {
  fe: "frontend · Vue 3 · Vite · Pinia"
  ms: "main-server · Spring Boot 4 · Java 17"
  pay: "payment-server · Spring Boot 4"
  ai: "ai-server · FastAPI · Python 3.13"
  cb: "chatbot-server · FastAPI + OpenAI"
  st: "storage-server · Go 1.24"
}
infra: "인프라" {
  pg: "PostgreSQL 16 · hakku DB" { shape: cylinder }
  rd: "Redis 7" { shape: cylinder }
  kf: "Kafka 3.7 (KRaft)" { shape: queue }
}
ext: "외부 연동" {
  oai: "OpenAI · gpt-image-2 / gpt-5.4-mini"
  toss: "토스페이먼츠 (PG)"
}
obs: "관측성" {
  pr: Prometheus
  gf: "Grafana :3000"
  jg: "Jaeger :16686"
}
client -> nginx
nginx -> services.fe: "/"
nginx -> services.ms: "/api/"
nginx -> services.pay: "/api/payments"
nginx -> services.ai: "/ai/"
nginx -> services.cb: "/chat/ (SSE)"
nginx -> services.st: "/storage/"
services.ms -> infra.pg
services.ms -> infra.rd
services.ms -> infra.kf
services.pay -> infra.pg
services.pay -> infra.kf
services.pay -> ext.toss: "결제 승인"
services.ai -> ext.oai
services.ai -> services.ms: "진단 반영"
services.ai -> services.st: "이미지"
services.cb -> ext.oai
services.cb -> services.ms: "고객센터 도구"
infra.kf -> services.ms: "payment.approved"
services.ms -> obs.pr: "/metrics"
obs.pr -> obs.gf
```

---

## 3. 기능 요구사항 (FR)

기능 요구사항은 팩트시트 §10 ID 스킴(`FR-{영역}-{n}`)을 따른다. 영역 코드는 다음과 같다: 01 회원/인증, 02 진단, 03 상품/커머스, 04 주문/결제, 05 추천, 06 커뮤니티, 07 알림, 08 매거진, 09 관리자, 10 챗봇. 우선순위는 필수(서비스 핵심 가치)·선택(부가 기능)으로 구분한다.

### 3.1 영역 01 — 회원/인증

| 요구사항ID | 요구사항명 | 상세 설명 | 우선순위 | 관련 화면 | 관련 UC |
|---|---|---|---|---|---|
| FR-01-01 | 회원가입 | 이메일·닉네임·비밀번호로 일반회원(NORMAL) 또는 판매자(SELLER) 가입. 이메일은 유일(UK), 비밀번호는 해시(`password_hash`) 저장. 가입 직후 `diagnosis_status=NONE`, `onboarding_completed=false` | 필수 | SCR-03 | UC-01 |
| FR-01-02 | 로그인 | 이메일·비밀번호 검증 후 JWT(액세스/리프레시) 발급. 인증 실패 시 오류 메시지 반환 | 필수 | SCR-02 | UC-02 |
| FR-01-03 | 로그아웃 | 토큰 무효화(`jwt:blacklist:{token}` 등록)로 세션 종료 | 필수 | SCR-19 | UC-02 |
| FR-01-04 | 토큰 재발급 | 리프레시 토큰으로 액세스 토큰 재발급(`POST /api/auth/refresh`) | 필수 | (전역) | UC-02 |
| FR-01-05 | 온보딩(선호 설정) | NORMAL 회원은 선호 컬러·스타일을 설정해야 함. 완료 시 `onboarding_completed=true`. 미완료 시 보호 경로 접근 차단(온보딩 게이트) | 필수 | SCR-04 | UC-03 |
| FR-01-06 | 내 정보 조회/수정 | `GET /api/users/me`, `PUT /api/users/me`로 프로필(닉네임·프로필 이미지 등) 조회·수정 | 필수 | SCR-19 | UC-04 |
| FR-01-07 | 선호 컬러 수정 | `PATCH /api/users/me/preferred-colors`로 선호 컬러(다대다) 갱신 | 선택 | SCR-04, SCR-19 | UC-03 |
| FR-01-08 | 프로필 조회 | `GET /api/users/{id}`로 타 사용자 프로필·활동·팔로워/팔로잉 조회 | 필수 | SCR-09 | UC-05 |
| FR-01-09 | 팔로우/언팔로우 | `POST /api/users/{id}/follow` 토글. 자기 자신 팔로우 불가(CHECK follower≠following), 중복 불가(UK) | 필수 | SCR-09 | UC-05 |
| FR-01-10 | 팔로워/팔로잉 목록 | `GET /api/users/{id}/followers`, `/following`로 관계 목록 조회 | 선택 | SCR-09 | UC-05 |
| FR-01-11 | 역할 기반 접근제어 | 라우트 가드(requiresAuth·guestOnly·requiresAdmin)와 서버 권한 검증으로 액터별 기능 접근 통제 | 필수 | (전역) | UC-02, UC-23 |

### 3.2 영역 02 — AI 퍼스널컬러 진단

| 요구사항ID | 요구사항명 | 상세 설명 | 우선순위 | 관련 화면 | 관련 UC |
|---|---|---|---|---|---|
| FR-02-01 | 진단 사진 업로드 | 사용자가 얼굴 사진을 업로드(`POST /ai/api/diagnosis`). ai-server가 JWT 검증 후 즉시 202 반환(비동기) | 필수 | SCR-11 | UC-06 |
| FR-02-02 | 진단 슬롯 잠금 | 진단 시작 시 `POST /api/users/me/diagnosis-request`로 상태 NONE→PENDING 전이. 중복 요청 시 409 | 필수 | SCR-11 | UC-06 |
| FR-02-03 | 진단 이미지 생성·저장 | 백그라운드에서 리사이즈→gpt-image-2 이미지 생성→storage에 `kind=result`로 저장 | 필수 | SCR-11 | UC-06 |
| FR-02-04 | 세부 타입 추출 | gpt-5.4-mini로 진단 텍스트 추출 후 16종 PersonalColorType ENUM으로 파싱 | 필수 | SCR-11 | UC-06 |
| FR-02-05 | 진단 결과 반영 | `PATCH /api/users/me/personal-color`로 결과 저장, 상태 PENDING→COMPLETED 전이 | 필수 | SCR-11, SCR-19 | UC-06 |
| FR-02-06 | 진단 실패 복구 | 파이프라인 예외 시 `DELETE /api/users/me/diagnosis-request`로 상태 →NONE 복구 | 필수 | SCR-11 | UC-06 |
| FR-02-07 | 진단 완료 알림 | 진단 완료 시 Kafka로 DIAGNOSIS_COMPLETE 알림 발행 | 필수 | SCR-18 | UC-06, UC-16 |
| FR-02-08 | 진단 결과 조회 | 마이페이지에서 퍼스널컬러·진단 이미지(`diagnosis_image_url`) 확인 | 필수 | SCR-19 | UC-07 |

### 3.3 영역 03 — 상품/커머스

| 요구사항ID | 요구사항명 | 상세 설명 | 우선순위 | 관련 화면 | 관련 UC |
|---|---|---|---|---|---|
| FR-03-01 | 상품 목록·검색 | `GET /api/products`로 목록 조회, 검색어·카테고리 필터 지원 | 필수 | SCR-05 | UC-08 |
| FR-03-02 | 상품 상세 조회 | `GET /api/products/{id}`로 상세(가격·키컬러·서브컬러·스타일·이미지·구매 URL) 조회 | 필수 | SCR-06 | UC-08 |
| FR-03-03 | 상품 등록(판매자) | SELLER가 `POST /api/products`로 본인 상품 등록(가격≥0, 스타일·컬러 다대다) | 필수 | SCR-10 | UC-09 |
| FR-03-04 | 상품 수정/삭제(판매자) | SELLER가 본인 상품을 `PUT`/`DELETE`. 비활성화는 `active=false` | 필수 | SCR-10 | UC-09 |
| FR-03-05 | 장바구니 담기 | `POST /api/cart/items`로 상품 추가(수량≥1, UK(user_id,product_id)) | 필수 | SCR-06, SCR-12 | UC-10 |
| FR-03-06 | 장바구니 조회/수정/삭제 | `GET/PUT/DELETE /api/cart/items`로 수량 변경·항목 제거 | 필수 | SCR-12 | UC-10 |
| FR-03-07 | 상품 찜(위시리스트) | `POST /api/products/{id}/wishlist`로 찜 토글(UK(product_id,user_id)) | 필수 | SCR-06, SCR-19 | UC-11 |
| FR-03-08 | 찜 목록 조회 | `GET /api/users/{id}/wishlist`로 본인/타인 찜 목록 조회 | 선택 | SCR-19, SCR-09 | UC-11 |
| FR-03-09 | 찜 좋아요 | `POST /api/wishlist/{id}/likes`로 타인 찜에 좋아요(UK(wishlist_id,user_id)). WISHLIST_LIKE 알림 발생 | 선택 | SCR-09 | UC-11, UC-16 |
| FR-03-10 | 리뷰 작성 | `POST /api/products/{id}/reviews`로 평점(1~5)·내용 작성(UK(product_id,author_id) — 상품당 1회) | 필수 | SCR-06 | UC-12 |
| FR-03-11 | 리뷰 수정/삭제 | `PUT/DELETE /api/reviews/{id}`로 본인 리뷰 수정·삭제 | 필수 | SCR-06 | UC-12 |
| FR-03-12 | 리뷰 조회 | `GET /api/products/{id}/reviews`, `GET /api/users/{userId}/reviews`로 상품·작성자별 리뷰 조회 | 필수 | SCR-06, SCR-09 | UC-12 |

### 3.4 영역 04 — 주문/결제

| 요구사항ID | 요구사항명 | 상세 설명 | 우선순위 | 관련 화면 | 관련 UC |
|---|---|---|---|---|---|
| FR-04-01 | 주문서 작성 | `POST /api/orders`로 배송지(수령인·연락처·우편번호·주소)·주문 항목 작성. 상태 CREATED | 필수 | SCR-13 | UC-13 |
| FR-04-02 | 주문 항목 스냅샷 | `order_items`에 상품명·가격을 스냅샷 저장(product_id FK 없음)하여 이후 상품 변경에 영향받지 않음 | 필수 | SCR-13 | UC-13 |
| FR-04-03 | 주문 조회 | `GET /api/orders`로 본인 주문 내역·상태(CREATED/PAID/CANCELLED) 조회 | 필수 | SCR-19 | UC-13 |
| FR-04-04 | 결제 인텐트 생성 | payment-server가 결제 PENDING 선기록(`idempotency_key` UK)으로 멱등 보장 | 필수 | SCR-14 | UC-14 |
| FR-04-05 | 결제 승인(토스 confirm) | 트랜잭션 밖에서 토스 confirm 호출 후 낙관적 락(@Version)으로 PENDING→APPROVED/FAILED 전이 + Outbox 기록 원자화 | 필수 | SCR-14, SCR-15 | UC-14 |
| FR-04-06 | 결제 위젯 | 프론트에서 토스페이먼츠 결제위젯 렌더링 및 결제 요청 | 필수 | SCR-14 | UC-14 |
| FR-04-07 | 결제 성공 처리 | 승인 성공 시 결제 성공 화면 표시 및 승인 결과 확인 | 필수 | SCR-15 | UC-14 |
| FR-04-08 | 결제 실패 처리 | 결제 실패 시 실패 안내 화면 표시(공개 접근) | 필수 | SCR-16 | UC-14 |
| FR-04-09 | PG 웹훅 수신 | `POST /api/payments/webhooks/pg`로 토스 웹훅 수신, HMAC-SHA256 서명 검증 | 필수 | (전역) | UC-15 |
| FR-04-10 | Outbox 이벤트 발행 | OutboxRelay가 `payment_outbox` 폴링→Kafka(payment.approved/failed) at-least-once 발행. 재시도 한계 초과 시 DEAD 격리 | 필수 | (전역) | UC-15 |
| FR-04-11 | 주문 PAID 전이 | main의 OrderPaymentConsumer가 payment.approved 구독→주문 상태 PAID 전이 + ORDER 알림 발생 | 필수 | SCR-19, SCR-18 | UC-15, UC-16 |

### 3.5 영역 05 — 추천

| 요구사항ID | 요구사항명 | 상세 설명 | 우선순위 | 관련 화면 | 관련 UC |
|---|---|---|---|---|---|
| FR-05-01 | 개인화 추천 제공 | `GET /api/recommendations`로 퍼스널컬러 맞춤 상품 추천 제공 | 필수 | SCR-17, SCR-01 | UC-17 |
| FR-05-02 | 추천 점수 산출 | RecommendationScoreCalculator가 퍼스널컬러 일치도 + 선호 스타일·컬러 일치도 + 최근 행동로그(클릭/찜/장바구니) + 상품 인기도·리뷰점수를 합산 | 필수 | SCR-17 | UC-17 |
| FR-05-03 | 설명 가능한 추천 | 점수 구성요소를 응답에 분해 포함하여 추천 근거를 화면에 표시 | 필수 | SCR-17 | UC-17 |
| FR-05-04 | 행동 로그 수집 | 클릭·찜·장바구니 행동을 Redis(`user:{id}:recent-actions`)에 기록하여 추천 입력으로 활용 | 필수 | (전역) | UC-17 |
| FR-05-05 | 계절 단위 환원 | 16종 세부 타입을 추천 시 4계절 단위로 환원하여 매칭 폭 확보 | 선택 | SCR-17 | UC-17 |

### 3.6 영역 06 — 커뮤니티

| 요구사항ID | 요구사항명 | 상세 설명 | 우선순위 | 관련 화면 | 관련 UC |
|---|---|---|---|---|---|
| FR-06-01 | 게시글 작성 | `POST /api/posts`로 자유게시판(GENERAL) 또는 학생증 자랑(STUDENT_ID) 게시글 작성(제목·내용·이미지) | 필수 | SCR-07 | UC-18 |
| FR-06-02 | 게시판 탭 분리 | 자유게시판/학생증 자랑을 `board` ENUM(GENERAL/STUDENT_ID)으로 분리 표시 | 필수 | SCR-07 | UC-18 |
| FR-06-03 | 게시글 상세·목록 | 게시글 목록 및 상세(`GET /api/posts`, `/{id}`) 조회 | 필수 | SCR-07, SCR-08 | UC-18 |
| FR-06-04 | 댓글 작성/관리 | `POST /api/posts/{postId}/comments`, `PUT/DELETE /api/comments/{id}`로 댓글 작성·수정·삭제. COMMENT 알림 발생 | 필수 | SCR-08 | UC-19, UC-16 |
| FR-06-05 | 게시글 좋아요 | `POST /api/posts/{postId}/likes` 토글(UK(post_id,user_id)). LIKE 알림 발생 | 필수 | SCR-08 | UC-19, UC-16 |
| FR-06-06 | 학생증 자랑 연관 상품 | 학생증 자랑 게시글에 연관 상품 첨부(`post_products`, sort_order). 게시글에서 상품 진입 | 필수 | SCR-08 | UC-18 |

### 3.7 영역 07 — 알림

| 요구사항ID | 요구사항명 | 상세 설명 | 우선순위 | 관련 화면 | 관련 UC |
|---|---|---|---|---|---|
| FR-07-01 | 알림 생성·발행 | main에서 이벤트 발생 시 Kafka topic `notification.created` 발행 | 필수 | (전역) | UC-16 |
| FR-07-02 | 알림 적재 | NotificationConsumer가 구독→Redis List(`user:{id}:notifications`, 최대 50)에 적재 | 필수 | (전역) | UC-16 |
| FR-07-03 | 알림함 조회 | `GET /api/notifications` 폴링으로 알림 목록 조회 | 필수 | SCR-18 | UC-16 |
| FR-07-04 | 알림 유형 분류 | NotificationType(COMMENT, LIKE, DIAGNOSIS_COMPLETE, FOLLOW, WISHLIST_LIKE, ORDER)별로 알림 구분 | 필수 | SCR-18 | UC-16 |

### 3.8 영역 08 — 매거진

| 요구사항ID | 요구사항명 | 상세 설명 | 우선순위 | 관련 화면 | 관련 UC |
|---|---|---|---|---|---|
| FR-08-01 | 매거진 목록 | `GET /api/magazines`로 발행분(published=true) 목록 조회. `display_order` 정렬 | 필수 | SCR-01 | UC-20 |
| FR-08-02 | 매거진 상세 | `GET /api/magazines/{id}`로 마크다운 본문(kicker·title·subtitle·content) 렌더링 | 필수 | SCR-20 | UC-20 |
| FR-08-03 | 매거진 상품 임베드 | 매거진 본문에 상품을 임베드하여 상품 상세로 진입 | 선택 | SCR-20 | UC-20 |
| FR-08-04 | 홈 매거진 캐러셀 | 홈 화면에서 매거진 캐러셀로 노출 | 필수 | SCR-01 | UC-20 |

### 3.9 영역 09 — 관리자

| 요구사항ID | 요구사항명 | 상세 설명 | 우선순위 | 관련 화면 | 관련 UC |
|---|---|---|---|---|---|
| FR-09-01 | 매거진 CRUD | ADMIN이 `/api/admin/magazines`로 매거진 생성·수정·삭제·발행 관리 | 필수 | SCR-21 | UC-21 |
| FR-09-02 | 상품 전체 관리 | ADMIN이 `/api/admin/products`로 전체 상품 조회·수정·삭제 | 필수 | SCR-22 | UC-22 |
| FR-09-03 | 관리자 접근 통제 | requiresAdmin 가드 및 서버 권한 검증으로 비ADMIN 접근 차단(→/) | 필수 | SCR-21, SCR-22 | UC-23 |

### 3.10 영역 10 — 챗봇

| 요구사항ID | 요구사항명 | 상세 설명 | 우선순위 | 관련 화면 | 관련 UC |
|---|---|---|---|---|---|
| FR-10-01 | 챗봇 진입(전역 FAB) | 거의 전 페이지 우측 하단 FAB로 학꾸AI 고객센터 챗봇 진입 | 필수 | (전역) | UC-24 |
| FR-10-02 | SSE 스트리밍 응답 | `/chat/stream`(SSE)로 토큰 단위 스트리밍 응답 제공 | 필수 | (전역) | UC-24 |
| FR-10-03 | 대화 기억 | Redis(`chat:history:{user_id}`, Sorted Set, 1시간 윈도우)로 대화 맥락 유지 | 필수 | (전역) | UC-24 |
| FR-10-04 | function-calling 도구 | get_order_history·get_wishlist·recommend_products 도구를 사용자 JWT로 main 호출(최대 3라운드) | 필수 | (전역) | UC-24 |
| FR-10-05 | 상품 카드 임베딩 | 챗봇 응답에 상품 카드를 임베딩하여 상품 진입 유도 | 선택 | (전역) | UC-24 |
| FR-10-06 | 대화 이력 조회 | `/chat/history`로 최근 대화 이력 조회 | 선택 | (전역) | UC-24 |

---

## 4. 비기능 요구사항 (NFR)

비기능 요구사항은 팩트시트 §10 ID 스킴(`NFR-{n}`)을 따르며 성능·보안·확장성·관측성·가용성·사용성 범주로 구분한다.

### 4.1 성능 (Performance)

| 요구사항ID | 항목 | 상세 설명 | 검증 기준 |
|---|---|---|---|
| NFR-01 | 이미지 처리량 | 이미지 입출력을 Go(`storage-server`)로 분리하여 고처리량 확보. 동일 API·JWT 정책 Spring 대조군 대비 처리량 우위 | 평소(20 VU): Go 3,610 req/s vs Spring 939 req/s (+284%, 약 3.8배). 고부하(100 VU): Go 2,340 req/s vs Spring 926 req/s (+153%) |
| NFR-02 | 업로드 지연(p95) | 이미지 업로드 p95 지연을 낮게 유지 | 평소(20 VU): Go 15ms vs Spring 75ms (+80%). 고부하(100 VU): Go 132ms vs Spring 479ms (+72%). Spring은 고부하에서 p95가 약 6배 증가 |
| NFR-03 | k6 부하 시나리오 | k6로 평소(20 VU)·고부하(100 VU) 시나리오 측정. 워크로드: 실제 이미지(72KB~865KB) 업로드→다운로드→삭제(`kind=raw`) | 격리된 공정 환경(CPU 2코어·메모리 512MB, 동일 JWT_SECRET, `compose/storage-bench.yml`, `scripts/benchmark-storage-fair.sh`) |
| NFR-04 | 비동기 진단 응답 | AI 진단은 즉시 202 반환 후 백그라운드 처리하여 요청 스레드 점유를 최소화 | `POST /ai/api/diagnosis` 202 즉시 반환 |
| NFR-05 | 캐시 응답 | 상품 등 조회 빈도 높은 데이터를 Redis(`product:{id}:cache`)로 캐시하여 DB 부하 절감 | 캐시 적중 시 DB 미조회 |

### 4.2 보안 (Security)

| 요구사항ID | 항목 | 상세 설명 |
|---|---|---|
| NFR-06 | JWT 인증 | 무상태 JWT로 인증. 로그아웃 시 `jwt:blacklist:{token}` 등록으로 토큰 무효화. ai-server·chatbot-server도 JWT 검증(`_require_auth`) |
| NFR-07 | result 이미지 접근제어 | `kind=result` 이미지는 JWT의 ownerId와 메타데이터 ownerId 일치 시에만 다운로드 허용. 불일치 시 403 |
| NFR-08 | 결제 웹훅 검증 | PG 웹훅(`/api/payments/webhooks/pg`)은 HMAC-SHA256 서명 검증으로 위·변조 차단 |
| NFR-09 | 멱등성 보장 | 결제는 `idempotency_key`(UK)로 중복 승인 방지. 동일 키 재요청 시 한 번 처리한 결과와 동일 |
| NFR-10 | 레이트 리밋 | 인증·결제 등 민감 엔드포인트에 레이트 리밋 적용으로 남용 방지 |
| NFR-11 | 토스 키 검증 | 토스 시크릿 키 유효성 검증(`PAYMENT_TOSS_SECRET_KEY_VALIDATION_ENABLED`). 샌드박스 테스트 시 비활성화 가능 |
| NFR-12 | 비밀 관리 | JWT_SECRET·OPENAI_API_KEY·PAYMENT_WEBHOOK_SECRET·TOSS_SECRET_KEY 등을 환경변수로 주입, 소스 하드코딩 금지 |
| NFR-13 | 권한 검증 | 라우트 가드(클라이언트)와 서버 권한 검증(이중)으로 액터별 기능·데이터 접근 통제 |

### 4.3 확장성 (Scalability)

| 요구사항ID | 항목 | 상세 설명 |
|---|---|---|
| NFR-14 | MSA 독립 배포 | 6개 서비스가 독립 실행·확장 가능. 트래픽 특성별로 개별 스케일 아웃 |
| NFR-15 | Kafka 비동기 | 결제·알림을 Kafka 이벤트로 비동기 처리하여 서비스 간 결합도 완화 및 처리량 확장 |
| NFR-16 | Redis 캐시·상태 | Redis로 알림 List·행동 로그·챗봇 대화·캐시·블랙리스트를 분산 관리 |
| NFR-17 | Storage 독립 확장 | 이미지 트래픽을 Nginx 라우팅으로 분리, storage-server를 비즈니스 서버와 독립적으로 확장 |

### 4.4 관측성 (Observability)

| 요구사항ID | 항목 | 상세 설명 |
|---|---|---|
| NFR-18 | 메트릭 노출 | main·payment·storage 백엔드가 `/metrics` 엔드포인트 노출 |
| NFR-19 | 메트릭 수집·시각화 | Prometheus가 메트릭 수집, Grafana(:3000) 대시보드(Service Overview·Storage 벤치마크)로 시각화 |
| NFR-20 | 분산 추적 | Jaeger(:16686)로 분산 트레이스 수집(현 시점 storage는 미연동, main 중심) |

### 4.5 가용성 (Availability)

| 요구사항ID | 항목 | 상세 설명 |
|---|---|---|
| NFR-21 | Outbox at-least-once | 트랜잭셔널 아웃박스로 DB 커밋과 이벤트 발행 원자화, OutboxRelay 재시도로 최소 1회 전달 보장 |
| NFR-22 | DEAD 격리 | 재시도 한계 초과 메시지를 DEAD 상태로 격리하여 정상 흐름 보호 및 수동 복구 가능 |
| NFR-23 | 진단 실패 복구 | 진단 파이프라인 예외 시 상태를 NONE으로 복구하여 재시도 가능, 잠긴 슬롯 해제 |

### 4.6 사용성 (Usability)

| 요구사항ID | 항목 | 상세 설명 |
|---|---|---|
| NFR-24 | 온보딩 게이트 | NORMAL 회원이 온보딩 미완료 상태로 보호 경로 접근 시 온보딩으로 강제 유도하여 추천 품질의 전제(선호 데이터) 확보 |
| NFR-25 | 설명 가능 추천 | 추천 점수의 구성요소를 분해 노출하여 사용자가 추천 근거를 이해할 수 있도록 함 |
| NFR-26 | 반응형 UI | Vue 3 + Tailwind 기반 반응형 화면(22개)으로 데스크톱·모바일 웹 대응 |

---

## 5. 제약사항 및 가정

### 5.1 제약사항

| 구분 | 제약 내용 |
|---|---|
| 기술 스택 고정 | 서비스별 런타임 고정: frontend(Vue 3+Vite+TS+Tailwind+Pinia), main/payment(Spring Boot 4.0.6/Java 17), ai/chatbot(FastAPI/Python 3.13), storage(Go 1.24). 인프라: PostgreSQL 16·Redis 7·Kafka 3.7(KRaft) |
| DB 공유 제약 | payment-server는 main과 동일 hakku DB 공유. Flyway 이력 테이블 분리(`flyway_schema_history_payment`, baseline 0)로 마이그레이션 충돌 방지 |
| 외부 API 의존 | OpenAI(gpt-image-2·gpt-5.4-mini) 가용성·요금에 진단 품질·비용이 종속. 토스페이먼츠는 샌드박스 환경에서 검증(`PAYMENT_TOSS_SECRET_KEY_VALIDATION_ENABLED=false` 옵션) |
| 프록시 구조 | 전 트래픽은 Nginx(:19001) 단일 진입점을 경유. 운영은 앞단 Caddy가 19001로 프록시 |
| 데이터셋 의존 | 진단·추천 시연은 외부 제공 `hakku_dataset.zip`을 `data/`에 배치해야 동작 |

### 5.2 가정

| 구분 | 가정 내용 |
|---|---|
| 팀 규모 | 2인 팀(천창현=Lead/BE·AI·Infra·FE, 김해찬=결제·챗봇·협업환경) |
| 일정 | 약 4주(2026-06-02 ~ 2026-06-25) 내 설계~검증 완료 |
| 운영 환경 | docker-compose 기반 단일 호스트 시연 환경을 전제(대규모 멀티노드 클러스터 미가정) |
| 사용자 입력 | 진단용 사진은 정면 얼굴 이미지를 가정. 비정상 입력은 실패 처리 후 NONE 복구 |
| 통화 | 결제 통화는 단일(`currency`) 기준으로 처리 |

---

## 6. 핵심 상태/흐름

### 6.1 AI 진단 상태머신

진단 상태(`users.diagnosis_status`)는 NONE→PENDING→COMPLETED로 전이하며, 파이프라인 실패 시 NONE으로 복구된다.

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

진단 파이프라인 요약: 업로드 → ai-server JWT 검증·202 즉시 반환 → 백그라운드(리사이즈 → gpt-image-2 이미지 생성 → storage `kind=result` 저장 → gpt-5.4-mini 세부 타입 텍스트 추출 → 16종 ENUM 파싱 → main `PATCH personal-color`, COMPLETED + Kafka DIAGNOSIS_COMPLETE 발행). 중복 요청은 409로 거부된다.

### 6.2 결제 Intent → Charge → Settle 요약

결제는 멱등·원자성·비동기 전파를 만족하는 3단계로 동작한다.

```d2
direction: right
intent: "① Intent\nPENDING 선기록\nidempotency_key (UK)"
charge: "② Charge\n트랜잭션 밖\n토스 confirm 호출"
settle: "③ Settle\n@Version 낙관적 락\nPENDING → APPROVED/FAILED\n+ Outbox 기록 (원자적)"
relay: "OutboxRelay 폴링\n→ Kafka payment.approved/failed\nat-least-once · 초과 시 DEAD"
consumer: "main OrderPaymentConsumer\n주문 PAID 전이 + ORDER 알림"
intent -> charge -> settle -> relay -> consumer
```

1. **Intent**: 결제 PENDING을 `idempotency_key`(UK)로 선기록하여 중복 승인을 방지한다.
2. **Charge**: DB 트랜잭션 밖에서 토스 confirm을 호출하여 외부 호출 지연이 락을 점유하지 않게 한다.
3. **Settle**: 낙관적 락(`@Version`)으로 PENDING→APPROVED/FAILED 전이와 `payment_outbox` 기록을 한 트랜잭션으로 원자화한다. OutboxRelay가 폴링하여 Kafka로 at-least-once 발행하고, 재시도 한계 초과 시 DEAD로 격리한다. main의 OrderPaymentConsumer가 payment.approved를 구독해 주문을 PAID로 전이하고 ORDER 알림을 발생시킨다.

---

## 7. 요구사항 추적 매트릭스

주요 기능 요구사항(FR)을 화면(SCR)·유스케이스(UC)와 상호 매핑한다. (상세 화면·유스케이스 정의는 후속 화면 설계서·유스케이스 명세를 참조)

| FR 영역 | 대표 FR | 관련 화면(SCR) | 관련 유스케이스(UC) | 관련 NFR |
|---|---|---|---|---|
| 01 회원/인증 | FR-01-01 회원가입, FR-01-02 로그인, FR-01-05 온보딩, FR-01-09 팔로우 | SCR-02, SCR-03, SCR-04, SCR-09, SCR-19 | UC-01, UC-02, UC-03, UC-04, UC-05 | NFR-06, NFR-13, NFR-24 |
| 02 진단 | FR-02-01 업로드, FR-02-05 결과 반영, FR-02-06 실패 복구 | SCR-11, SCR-19 | UC-06, UC-07 | NFR-04, NFR-07, NFR-23 |
| 03 상품/커머스 | FR-03-01 목록·검색, FR-03-05 장바구니, FR-03-07 찜, FR-03-10 리뷰 | SCR-05, SCR-06, SCR-10, SCR-12 | UC-08, UC-09, UC-10, UC-11, UC-12 | NFR-01, NFR-05 |
| 04 주문/결제 | FR-04-01 주문서, FR-04-05 결제 승인, FR-04-10 Outbox, FR-04-11 PAID 전이 | SCR-13, SCR-14, SCR-15, SCR-16, SCR-19 | UC-13, UC-14, UC-15 | NFR-08, NFR-09, NFR-11, NFR-21, NFR-22 |
| 05 추천 | FR-05-01 추천 제공, FR-05-02 점수 산출, FR-05-03 설명 가능 | SCR-17, SCR-01 | UC-17 | NFR-25 |
| 06 커뮤니티 | FR-06-01 게시글, FR-06-04 댓글, FR-06-05 좋아요, FR-06-06 연관 상품 | SCR-07, SCR-08 | UC-18, UC-19 | NFR-13 |
| 07 알림 | FR-07-01 발행, FR-07-02 적재, FR-07-03 조회 | SCR-18 | UC-16 | NFR-15, NFR-16 |
| 08 매거진 | FR-08-01 목록, FR-08-02 상세, FR-08-04 캐러셀 | SCR-01, SCR-20 | UC-20 | NFR-26 |
| 09 관리자 | FR-09-01 매거진 CRUD, FR-09-02 상품 관리, FR-09-03 접근 통제 | SCR-21, SCR-22 | UC-21, UC-22, UC-23 | NFR-13 |
| 10 챗봇 | FR-10-01 FAB, FR-10-02 SSE, FR-10-04 function-calling | (전역) | UC-24 | NFR-06, NFR-16 |

### 7.1 화면-요구사항 역추적 요약

| 화면(SCR) | 핵심 FR | 핵심 UC |
|---|---|---|
| SCR-01 HomeView | FR-08-04, FR-06-02, FR-05-01 | UC-20, UC-17 |
| SCR-02 LoginView | FR-01-02 | UC-02 |
| SCR-03 SignupView | FR-01-01 | UC-01 |
| SCR-04 OnboardingView | FR-01-05, FR-01-07 | UC-03 |
| SCR-05 ProductListView | FR-03-01 | UC-08 |
| SCR-06 ProductDetailView | FR-03-02, FR-03-05, FR-03-07, FR-03-10 | UC-08, UC-10, UC-11, UC-12 |
| SCR-07 CommunityView | FR-06-01, FR-06-02, FR-06-03 | UC-18 |
| SCR-08 PostDetailView | FR-06-03, FR-06-04, FR-06-05, FR-06-06 | UC-18, UC-19 |
| SCR-09 ProfileView | FR-01-08, FR-01-09, FR-01-10, FR-03-09 | UC-05 |
| SCR-10 SellerProductsView | FR-03-03, FR-03-04 | UC-09 |
| SCR-11 DiagnosisView | FR-02-01~FR-02-07 | UC-06 |
| SCR-12 CartView | FR-03-05, FR-03-06 | UC-10 |
| SCR-13 OrderFormView | FR-04-01, FR-04-02, FR-04-03 | UC-13 |
| SCR-14 PaymentCheckoutView | FR-04-04, FR-04-05, FR-04-06 | UC-14 |
| SCR-15 PaymentSuccessView | FR-04-07 | UC-14 |
| SCR-16 PaymentFailView | FR-04-08 | UC-14 |
| SCR-17 RecommendationView | FR-05-01, FR-05-02, FR-05-03, FR-05-05 | UC-17 |
| SCR-18 NotificationView | FR-07-03, FR-07-04 | UC-16 |
| SCR-19 MyPageView | FR-01-03, FR-01-06, FR-02-08, FR-04-03, FR-03-08 | UC-04, UC-07, UC-13 |
| SCR-20 MagazineDetailView | FR-08-02, FR-08-03 | UC-20 |
| SCR-21 AdminMagazineView | FR-09-01, FR-09-03 | UC-21, UC-23 |
| SCR-22 AdminProductsView | FR-09-02, FR-09-03 | UC-22, UC-23 |
| (전역) ChatFab | FR-10-01~FR-10-06 | UC-24 |
