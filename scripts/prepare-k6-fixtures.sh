#!/usr/bin/env bash
# coupang 상품 이미지(50~900KB) + 진단 raw(1~3MB) k6 픽스처 생성
set -euo pipefail
cd "$(dirname "$0")/.."
python3 scripts/_prepare_k6_fixtures.py
