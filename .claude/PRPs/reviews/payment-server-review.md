# 결제 기능 코드리뷰 — payment-server (+ 동반 변경)

**리뷰 일자:** 2026-06-24
**대상:** PR #20 `feat/payment-server` (Toss Payments 연동, Spring Boot 4.0.6 / Java 17, 트랜잭셔널 아웃박스 + Kafka) 및 같은 변경셋에 포함된 storage-server JWT 인증 추가
**방식:** 핵심 파일 직접 정독 전수 + security/java 전문 리뷰 2종 병렬 → 교차검증·중복제거·심각도 재보정
**최종 판정:** **REQUEST CHANGES** — 실제로 악용 가능한 CRITICAL 없음. 핵심 결제 보안통제는 견고. 실결제 트래픽 투입 전 HIGH 4건 수정 권장.
**다음 단계:** 본 문서 하단 [TDD 작업 계획](#tdd-작업-계획-새-세션용)을 새 세션에서 RED→GREEN→REFACTOR로 진행.

---

## ✅ 검증 완료 — 잘 된 것 (회귀 방지 대상)

결제 코드에서 흔히 터지는 항목들이 제대로 막혀 있음. 아래는 **이미 통과 중인 불변식**이므로, 수정 작업 시 깨지지 않도록 기존 테스트를 유지할 것.

| 통제 | 결과 | 근거 |
|---|---|---|
| 금액 위변조 | 차단 | `Payment.amount`·`TossConfirmRequest.amount` 모두 primitive `long` → `TossPaymentService.java:60`의 `!=`는 정상 값비교. 서버 확정금액 + Toss confirm 금액검증 이중 방어 |
| IDOR (남의 결제 confirm) | 차단 | `TossPaymentService.java:58` `.filter(p -> p.getUserId().equals(userId))`, 불일치는 404 |
| 웹훅 위조 | 차단 | raw 본문 HMAC-SHA256 + `MessageDigest.isEqual`(상수시간), 파싱 전 검증 |
| 이중과금/재전송 | 차단 | `idempotency_key` UNIQUE + `PaymentSettler` 멱등 no-op + `@Version` 낙관락 |
| 트랜잭셔널 아웃박스 | 정확 | 상태전이+이벤트기록이 동일 `@Transactional`(`PaymentSettler.settle`) |
| JWT | 정상 | jjwt 0.12.6 → 서명+만료 검증, `alg=none` 차단(확인), 약한 키 부팅 실패 |
| 시크릿 하드코딩 | 없음 | `JWT_SECRET`·`PAYMENT_WEBHOOK_SECRET` 기본값 無(fail-fast), 테스트 시크릿은 테스트 스코프 |
| SQLi / SSRF / 스택트레이스 노출 | 없음 | JPA 파생쿼리, 고정 base-url, 핸들러 일반화 메시지 |

**테스트 커버리지:** confirm 승인/거절/금액불일치/소유자불일치/replay/낙관락, 웹훅 서명/위조/멱등, 아웃박스 E2E(임베디드 Kafka)까지 핵심 경로 실질 커버. 우수.

---

## 🔴 HIGH — 실결제 투입 전 수정 권장

### H-1. Toss `RestClient`에 타임아웃 없음 → 워커 스레드 고갈
- **위치:** `payment-server/src/main/java/com/hakku/payment/config/RestClientConfig.java:16`
- **문제:** `RestClient.builder()`만 반환(요청 팩토리/타임아웃 미설정). Toss confirm 무응답 시 read timeout 무한 → 톰캣 워커 스레드 무기한 점유 → 결제 경로 마비. confirm을 트랜잭션 밖에서 호출한 설계는 좋으나 스레드 점유는 그대로.
- **수정 방향:** connect 3s / read 8s(Kafka SEND_TIMEOUT 10s·request.timeout 3s와 동일 규율) 명시.

### H-2. `TOSS_SECRET_KEY` 기본값으로 샌드박스 키가 박혀 있음 → 운영 무음 오작동
- **위치:** `payment-server/src/main/resources/application.properties:34`
- **문제:** `…secret-key=${TOSS_SECRET_KEY:test_gsk_docs_...}`. env 누락 시 에러 없이 샌드박스로 부팅 → 실결제 조용히 실패. (키 자체는 Toss 공개 문서 키 — 유출/로테이션 이슈 아님. 문제는 fail-fast 부재.)
- **수정 방향:** 기본값 제거 + 부팅 시 `test_` 접두/공백 키 거부 검증.

### H-3. storage-server `DELETE`/`GET …/meta` 인증 전무 *(동반 변경, 결제 외 영역)*
- **위치:** `storage-server/internal/api/api.go:134-149` (`stat`, `remove`)
- **문제:** `download`(116-125)는 result-kind 소유자 검증을 하는데 `remove`/`stat`은 인증·인가 0. id를 아는 누구나 임의 이미지 삭제·메타(ownerID) 열람 가능. id가 `crypto/rand` 32-hex라 추측은 어렵지만, 노출 시 비인증 파괴적 삭제. OWASP A01.
- **수정 방향:** `download`의 소유자 검증을 `remove`/`stat`에 미러링.

### H-4. 결제 엔드포인트 레이트리밋 없음
- **위치:** `PaymentController`, `TossPaymentController`, `PaymentWebhookController`
- **문제:** `prepare`/`confirm`/webhook 제한 없음 → PENDING 양산, orderId 존재 탐지(404 vs 400), 웹훅 DoS.
- **수정 방향:** Redis(이미 의존성) 기반 Bucket4j 등 인증사용자/IP 단위 제한. **단, 동반 nginx 인프라에서 처리될 수 있으니 그쪽 설정 확인 후 판단.**

---

## 🟡 MEDIUM

- **M-1. Actuator 전면 공개** — `SecurityConfig.java:32` permitAll + `application.properties:43-45` `show-details=always`·prometheus `unrestricted` → 비인증 인프라/메트릭(결제량·실패율·지연) 노출. → `show-details=when-authorized`, 별도 management 포트/IP 제한. (외부 포트 노출이면 HIGH)
- **M-2. 아웃박스 다중 인스턴스 동시성** — `outbox/OutboxRelay.java`의 `findTop100…`에 행 잠금 없음 → 인스턴스 2개↑ 시 매 폴링마다 Kafka 중복 발행. `FOR UPDATE SKIP LOCKED`(JPA `@Lock(PESSIMISTIC_WRITE)` + lock.timeout=0). 또한 배치가 첫 실패에서 끊겨 poison 이벤트 1건이 뒤 이벤트를 막음(retryCount/DEAD 부재).
- **M-3. 이벤트 시각이 전이시각 아님** — `payment/PaymentEvent.java:34` `Instant.now()` → 릴레이 지연 시 occurredAt 부정확. `payment.getUpdatedAt()` 사용.
- **M-4. 낙관락 재시도 1회·지터 없음 + 폴백 미흡** — `PaymentSettler` 재시도 + `GlobalExceptionHandler`. 두 writer 두 번 연속 충돌 시 `ObjectOptimisticLockingFailureException` 누수 → 500. catch-all 핸들러(409/503 매핑) + 소폭 지터.
- **M-5. `idempotencyKey`/outbox payload 길이 미검증** — `PaymentRequest.idempotencyKey` `@Size` 없음 → 255자 초과 시 DB 위반 → 500. `OutboxEvent.payload` VARCHAR(2000) 초과 동일. `@Size(max=128)` + payload `TEXT`로.
- **M-6. 웹훅 시크릿 최소 길이 미검증** — `WebhookSignatureVerifier.java:24`. 부팅 시 32바이트 미만 거부.
- **M-7. storage-server `JWT_SECRET` 누락이 치명 아님** — `cmd/storage-server/main.go`. 경고만 찍고 인증 없이 기동(result 이미지 공개). 운영 fatal 처리. *(동반 변경)*

---

## 🟢 LOW

- `IllegalArgumentException.getMessage()` 클라 반환(`GlobalExceptionHandler.java:44`) — 현재 안전한 도메인 문자열·JSON이라 XSS 아님, 웹훅 outcome 반향은 HMAC 게이트. 일반화 메시지 권장.
- Toss confirm 응답 `totalAmount` 교차검증 부재 — 서버 확정금액이 정본이라 방어심화 수준.
- `!=` primitive 비교(H-2 java 에이전트 지적)는 **현재 정확** — 향후 `Long` 승격 대비 주석/테스트만 보강.
- `MockPaymentGateway` 프로파일 미가드(`@Profile`/`@ConditionalOnMissingBean`), `Payment`/`OutboxEvent` `equals/hashCode` 부재, PG 실패 시에도 201 반환(`PaymentController`), `SecurityConfig` 보안헤더 명시화, 웹훅 타임스탬프 replay 윈도우, Go `safeID` 널바이트/hex-only, `PaymentKafkaConfig` 필드→생성자 주입.

---

## 검증 결과 표

| ID | 심각도 | 컴포넌트 | 요약 | 상태 |
|----|--------|----------|------|------|
| H-1 | HIGH | payment-server | Toss RestClient 타임아웃 없음 | 미수정 |
| H-2 | HIGH | payment-server | TOSS_SECRET_KEY 샌드박스 기본값(fail-fast 부재) | 미수정 |
| H-3 | HIGH | storage-server | DELETE/stat 인증 전무 | 미수정 |
| H-4 | HIGH | payment-server | 결제 엔드포인트 레이트리밋 없음 | 미수정(인프라 확인) |
| M-1 | MEDIUM | payment-server | Actuator 전면 공개 | 미수정 |
| M-2 | MEDIUM | payment-server | 아웃박스 SKIP LOCKED 부재 + poison 차단 | 미수정 |
| M-3 | MEDIUM | payment-server | 이벤트 시각이 전이시각 아님 | 미수정 |
| M-4 | MEDIUM | payment-server | 낙관락 재시도 1회 + catch-all 부재 | 미수정 |
| M-5 | MEDIUM | payment-server | idempotencyKey/payload 길이 미검증 | 미수정 |
| M-6 | MEDIUM | payment-server | 웹훅 시크릿 최소 길이 미검증 | 미수정 |
| M-7 | MEDIUM | storage-server | JWT_SECRET 누락이 fatal 아님 | 미수정 |
| (비이슈) | — | payment-server | jjwt alg=none → 0.12.6에서 차단 확인 | 안전 |

---

## TDD 작업 계획 (새 세션용)

> **워크플로:** 각 항목 RED(실패 테스트 먼저) → 실행해서 빨강 확인 → GREEN(최소 구현) → 초록 확인 → REFACTOR. `superpowers:test-driven-development` / `springboot-tdd` 스킬 사용 권장.
> **테스트 프레임:** JUnit 5 + AssertJ + Mockito + MockMvc (`payment-server/src/test/...`), Go는 `go test -race` (`storage-server/internal/api`).
> **권장 순서:** H-2 → H-1 → M-3 → M-5 → M-6 → M-4 → M-1 → M-2 → H-3 → H-4.
> (작은 결정적 단위부터; H-3/H-4는 별도 영역·인프라 확인 필요하므로 후순위)

### 작업 1 — H-2: TOSS_SECRET_KEY fail-fast
- **RED:** 신규 `config/TossSecretKeyValidatorTest.java`. `test_` 접두/공백 키 주입 시 `IllegalStateException`, 운영형 키는 통과.
- **GREEN:** 신규 `TossSecretKeyValidator`(`@PostConstruct` 또는 `ApplicationRunner`)에서 `secretKey.isBlank() || secretKey.startsWith("test_")` 거부. `application.properties:34` 기본값 제거(`${TOSS_SECRET_KEY}`).
- **주의:** `src/test/resources/application.properties:24`는 샌드박스 키를 명시 주입하므로, 검증을 운영 프로파일에만 걸거나 테스트 프로파일을 예외 처리해 `@SpringBootTest` 컨텍스트가 깨지지 않게 할 것.

### 작업 2 — H-1: RestClient 타임아웃
- **RED:** `gateway/toss/RestClientTossPaymentClientTest.java`에 케이스 추가 — 지연 응답 스텁(`MockRestServiceServer` 또는 느린 로컬 서버)에 대해 read timeout 내 `TossApiUnavailableException` 발생 검증.
- **GREEN:** `RestClientConfig.restClientBuilder()`에 connect 3s/read 8s 팩토리 설정. 타임아웃→`ResourceAccessException`→`RestClientException` 분기는 기존 `confirm()` catch가 `TossApiUnavailableException`으로 변환(확인).
- **REFACTOR:** 타임아웃 값 상수/프로퍼티화(`payment.toss.connect-timeout` 등).

### 작업 3 — M-3: 이벤트 시각
- **RED:** `payment/PaymentEventTest.java`(신규/확장) — `PaymentEvent.from(payment)`의 occurredAt이 `payment.getUpdatedAt().toEpochMilli()`와 같음.
- **GREEN:** `PaymentEvent.java:34` `Instant.now()` → `payment.getUpdatedAt()`.

### 작업 4 — M-5: 입력 길이 검증
- **RED(a):** `PaymentApiTest`에 129자 `idempotencyKey` → 400 케이스. **RED(b):** `OutboxEvent` 생성자에 2001자 payload → `IllegalStateException`(또는 마이그레이션 TEXT 전환 시 정상 저장).
- **GREEN:** `PaymentRequest.idempotencyKey`에 `@Size(max=128)` + `@Pattern("[A-Za-z0-9\\-_]+")`. `OutboxEvent` 생성자 길이 가드 또는 `V2__...` 마이그레이션으로 `payload TEXT`.

### 작업 5 — M-6: 웹훅 시크릿 최소 길이
- **RED:** `webhook/WebhookSignatureVerifierTest.java`에 31바이트 시크릿 주입 → 부팅/검증 시 `IllegalStateException`.
- **GREEN:** `WebhookSignatureVerifier`에 `@PostConstruct` 길이 가드(≥32). 테스트 프로퍼티 시크릿도 32바이트 이상으로 교체.

### 작업 6 — M-4: 낙관락 폴백 + catch-all
- **RED:** `PaymentSettlerTest`/서비스 테스트 — 두 번 연속 `ObjectOptimisticLockingFailureException` 시 동작 정의(예: 종료상태 재조회 멱등 or 명시적 409 매핑). `GlobalExceptionHandler` 테스트 — 미처리 `RuntimeException` → 500 단일 `ErrorResponse` 형태.
- **GREEN:** catch-all `@ExceptionHandler(Exception.class)` 추가(일반화 메시지+서버 로그). 재시도에 소폭 지터 또는 횟수 상향.

### 작업 7 — M-1: Actuator 보호
- **RED:** MockMvc — `/actuator/prometheus` 비인증 접근이 차단(401/403) 또는 별도 포트로 분리되었는지 검증.
- **GREEN:** `show-details=when-authorized`, prometheus access 제한, `SecurityConfig`에서 `/actuator/**` permitAll 축소 또는 `management.server.port` 분리.

### 작업 8 — M-2: 아웃박스 SKIP LOCKED
- **RED:** `OutboxRelayIntegrationTest` 확장 — 동시 2-relay 시뮬레이션에서 동일 이벤트 1회만 발행. 그리고 첫 SENT 후 재폴링 0건.
- **GREEN:** `OutboxRepository`에 `@Lock(PESSIMISTIC_WRITE)` + `lock.timeout=0` 쿼리. 폴링은 짧은 read-tx, 발행은 tx 밖, mark-sent 별도 tx로 분리(현재 3-bean 구조 활용). poison 대비 retryCount/DEAD 도입.

### 작업 9 — H-3: storage-server 인증 *(Go)*
- **RED:** `storage-server/internal/api`에 테이블 테스트 — `DELETE`/`GET …/meta`가 토큰 없을 때 401, result-kind 비소유자 토큰일 때 403.
- **GREEN:** `download`의 `bearerSubject`+owner 검증을 `remove`/`stat`에 미러링. `go test -race ./...`.

### 작업 10 — H-4: 레이트리밋
- **선행:** 동반 nginx 설정에 이미 레이트리밋이 있는지 확인. 없으면 Redis(Bucket4j) 필터.
- **RED:** 제한 초과 시 429 반환 검증. **GREEN:** 인증사용자/IP 단위 버킷.

---

## 테스트 실행 명령
```bash
# payment-server (Gradle)
cd payment-server && ./gradlew test
# 단일 클래스
./gradlew test --tests 'com.hakku.payment.payment.toss.TossPaymentServiceTest'
# 커버리지(JaCoCo 적용 시)
./gradlew test jacocoTestReport

# storage-server (Go)
cd storage-server && go test -race ./...
```

---

## 참고 — 본 리뷰의 한계
- 동적 분석/실제 부하 테스트 미수행(정적 리뷰 + 테스트 코드 검토 기반).
- nginx/compose/prometheus 인프라 설정은 정독 범위 밖 — H-4·M-1은 인프라 레이어 확인 후 최종 판정 필요.
- 커버리지 수치(JaCoCo)는 실행하지 않음 — 새 세션에서 `jacocoTestReport`로 80% 기준 확인 권장.
