# 학꾸 (Hakku)

AI 퍼스널컬러 기반 꾸미기 아이템 추천 커머스·커뮤니티 플랫폼.

사용자가 얼굴 사진을 업로드하면 AI가 16종 퍼스널컬러를 진단하고, 진단 결과와 활동 이력을 바탕으로 스티커·뱃지·꾸미기 아이템을 개인화 추천한다.

---

## 아키텍처

Nginx 리버스 프록시 뒤에 4개 서비스가 독립 실행되는 폴리글랏 마이크로서비스 구조.

| 서비스 | 스택 | 책임 |
|---|---|---|
| `frontend` | Vue 3 + Vite + TypeScript + Tailwind + Pinia | 사용자 화면 |
| `main-server` | Spring Boot 4.0.6 (Java 17) | 회원·인증·커뮤니티·상품·추천·알림 |
| `ai-server` | FastAPI (Python 3.13) | 퍼스널컬러 진단, OpenAI 연동 |
| `storage-server` | Go 1.24 (표준 라이브러리) | 이미지 저장·서빙 |

**지원 인프라** — PostgreSQL · Redis · Kafka (KRaft) · Prometheus · Grafana · Jaeger

---

## 주요 특징

### AI 진단 파이프라인

사용자가 사진을 올리면 AI Server에서 전체 파이프라인이 비동기로 실행된다.

```
유저 사진 업로드
→ 이미지 리사이즈 전처리
→ 퍼스널컬러 템플릿 + 원본 사진을 OpenAI gpt-image-1에 전달 → 분석 결과 이미지 생성
→ 결과 이미지 Storage Server 저장, 원본 폐기
→ Vision OCR로 결과 이미지에서 16종 퍼스널컬러 추출
→ Main Server에 결과 반영
→ Kafka → Redis → 사용자 알림
```

페이지를 이탈해도 요청은 백그라운드에서 처리되고, 완료되면 알림이 온다.

### 이미지 트래픽 분리

Main Server는 이미지 바이트를 다루지 않는다. Nginx 레벨에서 라우팅이 분리되어 이미지 업로드·다운로드가 비즈니스 서버를 통과하지 않는다.

```
[진단] Frontend → Nginx → AI Server → (내부) Storage Server
[상품] Frontend → Nginx → Storage Server
```

### 콘텐츠 기반 추천 엔진

퍼스널컬러 일치도, 선호 스타일 일치도, 최근 행동 로그(클릭·찜·장바구니), 상품 인기도·리뷰 점수를 합산해 점수를 산출한다. 점수 구성 요소를 분해해서 응답에 포함하므로 추천 이유를 설명할 수 있다.

### Storage Server Go vs Spring 비교

Go 표준 라이브러리로 구현한 `storage-server`와 동일 API를 제공하는 Spring Boot 구현체를 병렬로 제공한다. k6 부하 테스트로 두 구현체의 처리량·응답 시간을 직접 비교할 수 있다.

```bash
./scripts/benchmark-storage.sh
# 결과: k6/results/go.json, k6/results/spring.json
```

### 관측성

세 백엔드 모두 `/metrics` 엔드포인트를 노출한다. Prometheus가 메트릭을 수집하고 Grafana 대시보드로 시각화하며, Jaeger로 서비스 간 분산 추적이 가능하다.

---

## 실행

### 데이터셋 준비

> [Google Drive — hakku_dataset.zip 다운로드](https://drive.google.com/file/d/1OtBROPRBg4sGTOoLM843mMl1klmXItge/view?usp=sharing)

다운로드 후 프로젝트 루트에 압축 해제한다.

### 환경변수 설정

```bash
cp .env.example .env
# POSTGRES_PASSWORD, JWT_SECRET, OPENAI_API_KEY 값을 채운다
```

### 기동

```bash
# 전체 스택
docker compose up -d --build

# 관측성 스택 (선택)
docker compose -f docker-compose.obs.yml up -d
```

| 엔드포인트 | 주소 |
|---|---|
| 프론트엔드 | http://localhost |
| Main API | http://localhost/api |
| AI API | http://localhost/ai |
| Storage | http://localhost/storage |
| Grafana | http://localhost:3000 |
| Jaeger | http://localhost:16686 |
