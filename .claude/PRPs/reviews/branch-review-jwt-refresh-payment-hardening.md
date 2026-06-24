# Code Review: `feat/jwt-refresh-and-payment-hardening`

**Reviewed:** 2026-06-24
**Branch:** feat/jwt-refresh-and-payment-hardening → main
**Method:** 4 parallel domain reviewers (security cross-cutting, java, typescript, go) + synthesis
**Decision:** APPROVE with fixes applied (see below). 2 pre-existing issues flagged for follow-up.

## Summary
Refresh-token auth (15m/24h, httpOnly cookie), checkout-logout fix, and the payment-server
security review (HIGH+MEDIUM). Review surfaced one CRITICAL gap in the new auth work and
several HIGH/MEDIUM issues; all in-scope findings fixed under TDD. All 4 suites green.

## Findings

### CRITICAL — fixed
- **Refresh token usable as access token (payment-server + Go storage-server).** main-server's
  filter rejected `type=refresh`, but the other validators (shared `JWT_SECRET`, sig+exp only)
  did not → a 24h refresh token worked as a Bearer access credential.
  Fix: payment `JwtTokenProvider.isRefreshToken` + filter guard; Go `jwt.go` rejects `type=refresh`.
  Tests: `TossPaymentApiTest.rejectsRefreshTokenAsAccess`, `jwt_test.go TestSubject_RejectsRefreshToken`.

### HIGH — fixed
- **Rate-limit IP spoof** (`PaymentRateLimitFilter`): trusted client `X-Forwarded-For` → bucket bypass.
  Fix: trust nginx-set `X-Real-IP`, else `getRemoteAddr()`.
- **Unbounded rate-limit map** (`TokenBucketRateLimiter`): memory-exhaustion DoS via many keys.
  Fix: LRU bounded by `payment.rate-limit.max-keys` (default 100k).

### MEDIUM — fixed
- `validateRefresh`/`isRefreshToken` byte-identical (main) → `validateRefresh` delegates.
- `PaymentEvent.from` NPE risk on null `updatedAt` → null-safe fallback to `Instant.now()`.
- frontend: `/auth/logout` added to `AUTH_ENDPOINTS` (logout 401 no longer triggers refresh-retry).
- main-server actuator `health.show-details=always` → `when-authorized`.

### HIGH (pre-existing) — FIXED (follow-up 2026-06-24)
- **Self-assignable ADMIN at `/api/auth/signup`** (`role` was taken from body). Genuine privilege-escalation.
  Fix: `AuthService.signup` rejects `Role.ADMIN` → `AdminSignupForbiddenException` (→ 403). The initial
  admin is now provisioned at startup by `AdminSeeder` from `app.admin.*` env config (idempotent; fail-fast
  if `app.admin.email` is set but `APP_ADMIN_PASSWORD` is blank; no-op when unset). `CurationCardApiTest`
  provisions its ADMIN via `UserRepository` instead of HTTP signup. NORMAL/SELLER self-signup unchanged.
  Tests: `AuthServiceTest.signupRejectsAdminRole`, `AuthApiTest.signupRejectsAdminRole` (403),
  `AdminSeederTest` (seed / idempotent / blank-email no-op / blank-password fail-fast). Full suite green.

### LOW (informational) — FLAGGED
- Refresh-token **role staleness / no revocation** (≤24h) — inherent to the chosen stateless model.
  Mitigation if desired: DB role lookup on refresh, or a `jti` blocklist.
- Multi-instance rate limiting is per-instance (in-memory); `RateLimiter` is the seam for a Redis impl.
- Outbox multi-instance dedup (SKIP LOCKED) deferred — needs Postgres claiming + Testcontainers.

### Evaluated, intentionally not changed
- TS "refreshPromise `.finally` race" (reviewer rated HIGH): proposed fix was incorrect (would reuse a
  stale token next cycle); real impact is at most one benign redundant refresh. Left as-is.
- TS "`original.headers` null guard": `status===401` implies the request was dispatched (headers set).

## Validation
| Check | Result |
|---|---|
| main-server `./gradlew test` | Pass |
| payment-server `./gradlew test` | Pass |
| storage-server `go build ./... && go test -race ./...` | Pass |
| frontend `vitest run` | Pass (46/46) |
