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
| `frontend` | Vue 3 + Vite + Tailwind | 사용자 화면 | `/` |
| `main-server` | Spring Boot 3.x (Java 17) | 회원·인증·커뮤니티·상품·추천·알림 | `/api/**` |
| `ai-server` | FastAPI (Python 3.13) | 퍼스널컬러 진단, OpenAI 연동 | `/ai/**` |
| `storage-server` | Go | 이미지 업로드/저장/삭제 | `/storage/**` |

지원 인프라: PostgreSQL · Redis · Kafka(KRaft) · Prometheus · Grafana · Jaeger.

## 디렉터리 구조

```
hakku/
├── docker-compose.infra.yml   # pg + redis + kafka (로컬 인프라)
├── .env.example               # 환경변수 템플릿
├── main-server/               # Spring Boot
├── ai-server/                 # FastAPI       (예정)
├── storage-server/            # Go            (예정 — go 미설치)
├── frontend/                  # Vue 3         (예정)
└── nginx/                     # 리버스 프록시  (예정)
```

## 로컬 개발 시작

데이터셋 다운로드 후 

```bash
# 1. 환경변수 준비
cp .env.example .env            # 시크릿 값 채우기

# 2. 인프라 기동
docker compose -f docker-compose.infra.yml up -d

# 3. 메인 서버 (Spring Boot)
cd main-server && ./gradlew bootRun
```

## 구현 진행 상황

TDD(테스트 우선) + 단계별 구현. 자세한 단계는 PR/커밋 히스토리 참고.

- [x] Phase 0 — 모노레포 + 로컬 인프라 스캐폴딩
- [x] Phase 1 — Main Server 코어 (추천 점수기 · 16종 퍼스널컬러 · User 도메인/스키마)
- [x] Phase 2 — 인증 (JWT-only; OAuth 제외) — signup/login/SecurityConfig/JWT 필터
- [ ] Phase 3 — 회원 / 커뮤니티  ← **다음 작업**
- [ ] Phase 4 — 상품 / 장바구니 / 리뷰
- [ ] Phase 5 — Storage Server (Go)
- [ ] Phase 6 — AI Server (FastAPI)
- [ ] Phase 7 — 추천 엔진
- [ ] Phase 8 — 알림
- [ ] Phase 9 — Frontend (Vue)
- [ ] Phase 10 — 관측성 / 부하 테스트
