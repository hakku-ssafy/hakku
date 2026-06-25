-- 상품 활성화 여부. false 면 공개 목록·검색·추천에서 숨긴다(어드민에서만 노출).
-- 기존 상품은 모두 활성(true)으로 시작한다.
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_products_active ON products (active);
