#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
[[ -f .env ]] || { echo ".env 필요 (JWT_SECRET)" >&2; exit 1; }
VUS="${VUS:-20}"
DURATION="${DURATION:-60s}"
GO_URL="${GO_URL:-http://localhost:8081}"
SPRING_URL="${SPRING_URL:-http://localhost:8082}"
mkdir -p k6/results
echo "==> 기존 storage 컨테이너 중지 (포트 8081/8082)"
docker stop hakku-storage-server hakku-storage-spring 2>/dev/null || true
echo "==> 격리 벤치 환경 (JWT 동일, 2CPU/512MB)"
docker compose -f compose/storage-bench.yml --env-file .env up -d --build --wait
echo "==> Observability"
docker compose -f compose/obs.yml up -d prometheus grafana jaeger 2>/dev/null || true
sleep 5
K6=(docker run --rm -i --network host -v "$ROOT/k6:/scripts" -w /scripts grafana/k6 run)
warmup() { local l="$1" u="$2"; echo "  warmup: $l"; "${K6[@]}" -e STORAGE_URL="$u" -e RUN_LABEL="warmup-$l" -e VUS=5 -e DURATION=15s -e OUT_DIR=results --quiet storage-bench.js >/dev/null; }
run_bench() { local l="$1" u="$2"; echo "==> k6: $l ($u VUS=$VUS $DURATION)"; "${K6[@]}" -e STORAGE_URL="$u" -e RUN_LABEL="$l" -e VUS="$VUS" -e DURATION="$DURATION" -e OUT_DIR=results --quiet storage-bench.js >/dev/null; }
warmup spring "$SPRING_URL"; warmup go "$GO_URL"; sleep 3
run_bench spring "$SPRING_URL"; sleep 5; run_bench go "$GO_URL"
python3 <<'INNER'
import json
from pathlib import Path

def load(n):
    return json.loads(Path(f"k6/results/results-{n}.json").read_text())

spring, go = load("spring"), load("go")

def fmt_ms(v):
    return "n/a" if v is None else f"{v:.1f}ms"

def fmt_val(key, v):
    if v is None:
        return "n/a"
    if key == "rps":
        return f"{v:.0f}"
    if key == "iterPerSec":
        return f"{v:.1f}"
    return fmt_ms(v)

print("\n======== 공정 벤치마크 (JWT 동일, 격리) ========")
print(f"{'지표':<22} {'Spring':>12} {'Go':>12} {'개선':>10}")
print("-" * 60)
for label, key, higher in [
    ("처리량 req/s", "rps", True),
    ("iter/s", "iterPerSec", True),
    ("업로드 p95", "uploadP95", False),
    ("업로드 p99", "uploadP99", False),
    ("다운로드 p95", "downloadP95", False),
    ("다운로드 p99", "downloadP99", False),
]:
    s, g = spring.get(key), go.get(key)
    if s is None or g is None:
        print(f"{label:<22} {fmt_val(key, s):>12} {fmt_val(key, g):>12} {'n/a':>10}")
        continue
    imp = (g - s) / s * 100 if higher else (s - g) / s * 100
    print(f"{label:<22} {fmt_val(key, s):>12} {fmt_val(key, g):>12} {imp:>+9.1f}%")
print(f"오류율 Spring={spring.get('errorRate')} Go={go.get('errorRate')}")
print("Grafana: http://localhost:3000/d/hakku-storage-bench")
INNER
