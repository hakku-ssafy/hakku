# 학꾸 (Hakku)

AI 퍼스널컬러 기반 꾸미기 아이템 추천 커머스/커뮤니티 플랫폼.

사용자는 이미지를 업로드해 AI 퍼스널컬러 진단을 받고, 진단 결과와 활동 이력을 기반으로
개인화된 스티커·뱃지·꾸미기 아이템을 추천받는다.

> 전체 기능/아키텍처 명세는 [`prd.md`](./prd.md) 참고.

## 아키텍처

폴리글랏 마이크로서비스. Nginx 리버스 프록시 뒤에 4개 서비스가 독립 실행되며,
Kafka로 비동기 이벤트를, Redis로 캐시/알림/행동 로그를 처리한다.

| 서비스 | 스택 | 책임 | 라우팅 |
| --- | --- | --- | --- |
| `frontend` | Vue 3 + Vite + TypeScript + Tailwind + Pinia | 사용자 화면 | `/` |
| `main-server` | Spring Boot 4.0.6 (Java 17) | 회원·인증·커뮤니티·상품·추천·알림 | `/api/**` |
| `ai-server` | FastAPI (Python 3.13) | 퍼스널컬러 진단, OpenAI 연동 | `/api/diagnosis` |
| `storage-server` | Go (1.26, 표준 라이브러리) | 이미지 업로드/저장/삭제 | `/storage/**` |

지원 인프라: PostgreSQL · Redis · Kafka(KRaft) · Prometheus · Grafana · Jaeger.

관측성은 `docker-compose.obs.yml`로 별도 기동하며, 세 백엔드(main/storage/ai) 모두
`/metrics` 또는 actuator Prometheus 엔드포인트를 노출한다. 부하 테스트는 `k6/`.

## 디렉터리 구조

```
hakku/
├── docker-compose.infra.yml   # pg + redis + kafka (로컬 인프라)
├── docker-compose.obs.yml     # prometheus + grafana + jaeger (관측성)
├── .env.example               # 환경변수 템플릿
├── main-server/               # Spring Boot 4 (회원·인증·커뮤니티·상품·추천·알림)
├── ai-server/                 # FastAPI (퍼스널컬러 진단 파이프라인)
├── storage-server/            # Go (파일시스템 이미지 저장)
├── frontend/                  # Vue 3 + Vite + TypeScript
├── observability/             # prometheus/grafana 프로비저닝 설정
├── k6/                        # 부하 테스트 시나리오
└── nginx/                     # 리버스 프록시 (예정)
```

## 로컬 개발 시작

데이터셋 다운로드 후 루트에 위치
[구글 드라이브](https://drive.google.com/file/d/1OtBROPRBg4sGTOoLM843mMl1klmXItge/view?usp=sharing)

```bash
# 1. 환경변수 준비
cp .env.example .env            # 시크릿 값 채우기

# 2. 인프라 기동
docker compose -f docker-compose.infra.yml up -d

# 3. 메인 서버 (Spring Boot)
cd main-server && ./gradlew bootRun

# 4. AI 서버 (FastAPI)
cd ai-server && .venv/bin/python -m uvicorn app.main:app --reload

# 5. 스토리지 서버 (Go)
cd storage-server && go run ./cmd/storage-server

# 6. 프론트엔드 (Vue)
cd frontend && npm install && npm run dev

# (선택) 관측성 스택 — Prometheus/Grafana/Jaeger
docker compose -f docker-compose.obs.yml up -d
```

### 테스트

```bash
cd main-server && ./gradlew test       # Spring Boot (153 tests)
cd ai-server && .venv/bin/python -m pytest   # FastAPI (44 tests, 92% cov)
cd storage-server && go test ./...     # Go (api/storage/metrics 3 pkg)
cd frontend && npm test                # Vue (Vitest, 23 tests)
```

## 구현 진행 상황

TDD(테스트 우선) + 단계별 구현. 자세한 단계는 PR/커밋 히스토리 참고.

- [x] Phase 0 — 모노레포 + 로컬 인프라 스캐폴딩
- [x] Phase 1 — Main Server 코어 (추천 점수기 · 16종 퍼스널컬러 · User 도메인/스키마)
- [x] Phase 2 — 인증 (JWT-only; OAuth 제외) — signup/login/SecurityConfig/JWT 필터
- [x] Phase 3 — 회원 / 커뮤니티 (프로필 `/api/users/me` · 게시글·댓글 CRUD/작성자권한 · 좋아요 토글)
- [x] Phase 4 — 상품 / 장바구니 / 리뷰 (판매자권한 상품 CRUD · 장바구니 토글/수량 · 리뷰 CRUD/중복방지)
- [x] Phase 5 — Storage Server (Go) — 파일시스템 이미지 저장 (`/storage` 업로드·다운로드·삭제·메타 · FSStore)
- [x] Phase 6 — AI Server (FastAPI) — 퍼스널컬러 진단 파이프라인 (`POST /api/diagnosis` · 전처리·Vision OCR·gpt-image-1 생성·storage/main 연동)
- [x] Phase 7 — 추천 엔진 (`GET /api/recommendations` · 회원 프로필+상품 피처 점수화/정렬 · 설명 가능한 점수 분해)
- [x] Phase 8 — 알림 (Kafka `notification.created` 발행/소비 · Redis 저장 · `GET /api/notifications` · 댓글/좋아요 이벤트 연동)
- [x] Phase 9 — Frontend (Vue 3 + Vite + TS + Pinia · 10개 View · Axios JWT 인터셉터 · 라우터 가드)
- [x] Phase 10 — 관측성 / 부하 테스트 (Prometheus · Grafana 대시보드 · Jaeger · k6 부하 시나리오 · 3개 서버 `/metrics`)

**모든 Phase(0~10) 구현 완료.** 테스트 전부 GREEN — main 153 · ai 44(92%) · go 3pkg · vue 23.
