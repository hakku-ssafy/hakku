"""data/coupang_images의 실제 상품 이미지를 k6 픽스처로 복사하고 manifest를 생성한다.

manifest.json 구조는 k6/lib/image-payload.js가 읽는 형식과 일치:
  files:   [{path, contentType, sizeBytes}]
  weights: [{path, kind, weight}]
"""
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "data" / "coupang_images"
DST = ROOT / "k6" / "fixtures"

# 크기 구간(bytes)과 가중치 — 소형 썸네일이 흔하고 대형 원본은 드물게
BUCKETS = [
    ("xs", 0, 100_000, 0.30),
    ("sm", 100_000, 250_000, 0.30),
    ("md", 250_000, 500_000, 0.20),
    ("lg", 500_000, 800_000, 0.12),
    ("xl", 800_000, 10_000_000, 0.08),
]


def content_type(path: Path) -> str:
    return "image/png" if path.suffix.lower() == ".png" else "image/jpeg"


def main() -> None:
    images = sorted(
        p for p in SRC.rglob("*") if p.suffix.lower() in (".jpg", ".jpeg", ".png")
    )
    if not images:
        raise SystemExit(f"no images found under {SRC} — README의 hakku_dataset.zip을 data/에 풀어주세요")

    DST.mkdir(parents=True, exist_ok=True)
    for old in DST.glob("*"):
        old.unlink()

    files, weights = [], []
    for name, lo, hi, weight in BUCKETS:
        candidates = [p for p in images if lo <= p.stat().st_size < hi]
        if not candidates:
            continue
        # 구간 중앙값에 가장 가까운 실제 이미지 선택 (재현성 위해 결정적)
        candidates.sort(key=lambda p: p.stat().st_size)
        chosen = candidates[len(candidates) // 2]
        dst_name = f"{name}{chosen.suffix.lower()}"
        shutil.copyfile(chosen, DST / dst_name)
        size = chosen.stat().st_size
        rel = f"fixtures/{dst_name}"
        files.append({"path": rel, "contentType": content_type(chosen), "sizeBytes": size})
        weights.append({"path": rel, "kind": "raw", "weight": weight})
        print(f"  {name}: {size/1024:.0f}KB ← {chosen.name}")

    total = sum(w["weight"] for w in weights)
    for w in weights:
        w["weight"] = round(w["weight"] / total, 4)

    manifest = {"files": files, "weights": weights}
    (DST / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2))
    print(f"OK: {len(files)} fixtures → {DST.relative_to(ROOT)}/")


if __name__ == "__main__":
    main()
