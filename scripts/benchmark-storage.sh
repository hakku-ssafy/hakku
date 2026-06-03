#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

BASE_URL="${BASE_URL:-http://localhost:19001}"
K6_BASE_URL="${K6_BASE_URL:-http://localhost:19001}"
GO_STORAGE_URL="${GO_STORAGE_URL:-http://localhost:19001}"  # k6 only
SPRING_STORAGE_URL="${SPRING_STORAGE_URL:-http://localhost:8082}"
EMAIL="${BENCH_EMAIL:-bench-$(date +%s)@hakku.dev}"
PASSWORD="${BENCH_PASSWORD:-bench-secret123}"

mkdir -p k6/results

echo "==> JWT 발급 (${EMAIL})"
SIGNUP_CODE=$(curl -s -o /tmp/hakku-signup.json -w '%{http_code}' \
  -X POST "${BASE_URL}/api/auth/signup" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\",\"nickname\":\"bench\",\"role\":\"NORMAL\"}")

if [[ "$SIGNUP_CODE" != "201" && "$SIGNUP_CODE" != "409" ]]; then
  echo "signup failed: HTTP ${SIGNUP_CODE}" >&2
  cat /tmp/hakku-signup.json >&2
  exit 1
fi

TOKEN=$(curl -s -X POST "${BASE_URL}/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\"}" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("accessToken",""))')

if [[ -z "$TOKEN" ]]; then
  echo "login failed: empty accessToken" >&2
  exit 1
fi

K6=(docker run --rm -i --network host \
  -v "$ROOT/k6:/scripts" -w /scripts grafana/k6 run)

run_case() {
  local label="$1"
  local storage_url="$2"
  echo ""
  echo "==> k6 실행: ${label} (storage=${storage_url})"
  "${K6[@]}" -e BASE_URL="$K6_BASE_URL" -e STORAGE_URL="$storage_url" \
    -e TEST_JWT="$TOKEN" -e RUN_LABEL="$label" architecture-load.js
  mv -f "results-${label}.json" "results/${label}.json" 2>/dev/null || true
}

run_case go "$GO_STORAGE_URL"
run_case spring "$SPRING_STORAGE_URL"

python3 <<'INNER'
import json
from pathlib import Path

rows = []
for name in ['go', 'spring']:
    p = Path('k6/results') / f'{name}.json'
    if not p.exists():
        p = Path('k6') / f'results-{name}.json'
    rows.append(json.loads(p.read_text()))

def fmt_ms(v):
    if v is None:
        return 'n/a'
    return f'{v:.0f}ms'

def fmt_pct(v):
    if v is None:
        return 'n/a'
    return f'{v*100:.2f}%'

print('\n================ Storage 벤치마크 비교 ================')
print(f"{'지표':<28} {'Go':>12} {'Spring':>12}")
print('-' * 54)
keys = [
    ('총 HTTP 요청', 'httpReqs', lambda v: str(int(v))),
    ('HTTP 오류율', 'httpReqFailedRate', fmt_pct),
    ('전체 P95', 'httpDurationP95', fmt_ms),
    ('전체 P99', 'httpDurationP99', fmt_ms),
    ('상품 P95', 'productP95', fmt_ms),
    ('추천 P95', 'recommendP95', fmt_ms),
    ('알림 P95', 'notificationP95', fmt_ms),
    ('Storage 업로드 P95', 'uploadP95', fmt_ms),
    ('Storage 업로드 avg', 'uploadAvg', fmt_ms),
]
for label, key, fn in keys:
    print(f"{label:<28} {fn(rows[0].get(key)):>12} {fn(rows[1].get(key)):>12}")

upload_go = rows[0].get('uploadP95') or 0
upload_sp = rows[1].get('uploadP95') or 0
if upload_go and upload_sp:
    diff = ((upload_sp - upload_go) / upload_go) * 100
    winner = 'Go' if upload_go < upload_sp else 'Spring'
    slower = 'Spring' if upload_sp > upload_go else 'Go'
    print(f"\nStorage 업로드 P95: {winner} 우세 ({abs(diff):.1f}% — {slower}이(가) 더 느림)")
INNER
