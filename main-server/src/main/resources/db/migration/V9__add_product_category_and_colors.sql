-- 상품 카테고리 및 컬러 태그 (color_1~6), 회원 선호 컬러, 진단 결과 이미지
ALTER TABLE products
    ADD COLUMN category VARCHAR(100);

CREATE TABLE product_colors (
    product_id BIGINT       NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    color      VARCHAR(40)  NOT NULL,
    PRIMARY KEY (product_id, color)
);

CREATE INDEX idx_products_category ON products (category);

CREATE TABLE user_preferred_colors (
    user_id BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    color   VARCHAR(40)  NOT NULL,
    PRIMARY KEY (user_id, color)
);

ALTER TABLE users
    ADD COLUMN diagnosis_image_url VARCHAR(1024);
