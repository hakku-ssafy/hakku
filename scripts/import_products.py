#!/usr/bin/env python3
"""Import all Coupang products from coupang_products.xlsx into PostgreSQL."""
from __future__ import annotations
import random
import re
import subprocess
import sys
from pathlib import Path
from urllib.parse import parse_qs, urlencode, urlparse, urlunparse
import pandas as pd

ROOT = Path(__file__).resolve().parents[1]
XLSX = ROOT / "data" / "coupang_images" / "coupang_products.xlsx"
if not XLSX.exists():
    XLSX = ROOT / "data" / "coupang_products.xlsx"  # fallback
if not XLSX.exists():
    XLSX = ROOT / "coupang_products.xlsx"  # fallback: 루트에 파일이 있는 경우
# 상대경로 기본값 — 게이트웨이 nginx의 /product-images/ 로컬 서빙을 통해 호스트 무관하게 동작
PUBLIC_BASE = sys.argv[1] if len(sys.argv) > 1 else "/product-images"
# bcrypt hash for the seed password "password"; these accounts are fixture sellers.
SELLER_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
SELLER_EMAILS = [f"seller{i:02d}@hakku.local" for i in range(1, 21)]
RANDOM_SEED = 20260625

def parse_price(raw):
    digits = re.sub(r"[^0-9]", "", str(raw or ""))
    return int(digits) if digits else 0

def image_url(image_file):
    if image_file is None or (isinstance(image_file, float) and pd.isna(image_file)): return None
    path = str(image_file).replace("\\", "/")
    if path.startswith("coupang_images/"): path = path[len("coupang_images/"):]
    return f"{PUBLIC_BASE.rstrip('/')}/{path}"

def esc(value: str) -> str:
    return value.replace("'", "''")

def like_esc(value: str) -> str:
    return esc(value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"))

def sql_text(value):
    if value is None or (isinstance(value, float) and pd.isna(value)):
        return "NULL"
    text = str(value).strip()
    return f"'{esc(text)}'" if text else "NULL"

def sql_literal(value: str) -> str:
    return f"'{esc(value)}'"

def cell_text(value, default: str = "") -> str:
    if value is None or (isinstance(value, float) and pd.isna(value)):
        return default
    text = str(value).strip()
    return text if text else default

def canonical_purchase_url(value):
    if value is None or (isinstance(value, float) and pd.isna(value)):
        return None, None
    text = str(value).strip()
    if not text:
        return None, None

    parsed = urlparse(text)
    parts = [part for part in parsed.path.split("/") if part]
    query = parse_qs(parsed.query)
    if len(parts) >= 3 and parts[0] == "vp" and parts[1] == "products":
        product_id = parts[2]
        item_id = query.get("itemId", [""])[0]
        vendor_item_id = query.get("vendorItemId", [""])[0]
        if item_id and vendor_item_id:
            canonical_query = urlencode(
                {"itemId": item_id, "vendorItemId": vendor_item_id}
            )
            canonical_url = urlunparse(
                (
                    parsed.scheme or "https",
                    parsed.netloc or "www.coupang.com",
                    f"/vp/products/{product_id}",
                    "",
                    canonical_query,
                    "",
                )
            )
            return canonical_url, (product_id, item_id, vendor_item_id)

    return text, None

def purchase_url_duplicate_check(link_sql: str, key) -> str:
    if key is None:
        return f"purchase_url = {link_sql}"
    product_id, item_id, vendor_item_id = key
    return (
        "substring(purchase_url from '/vp/products/([^/?&]+)') = "
        f"{sql_literal(product_id)} "
        "AND substring(purchase_url from '[?&]itemId=([^&]+)') = "
        f"{sql_literal(item_id)} "
        "AND substring(purchase_url from '[?&]vendorItemId=([^&]+)') = "
        f"{sql_literal(vendor_item_id)}"
    )

def run_psql(sql: str) -> None:
    subprocess.run(
        [
            "docker",
            "exec",
            "-i",
            "hakku-postgres",
            "psql",
            "-v",
            "ON_ERROR_STOP=1",
            "-U",
            "hakku",
            "-d",
            "hakku",
        ],
        input=sql,
        text=True,
        check=True,
    )

def main():
    df = pd.read_excel(XLSX)
    rng = random.Random(RANDOM_SEED)
    print(f"Importing up to {len(df)} products from {XLSX}")

    stmts = ["BEGIN;"]
    for idx, email in enumerate(SELLER_EMAILS, start=1):
        stmts.append(
            "INSERT INTO users (email,nickname,password_hash,role,diagnosis_status) "
            f"VALUES ('{email}','학꾸 판매자 {idx:02d}','{SELLER_HASH}','SELLER','NONE') "
            "ON CONFLICT (email) DO NOTHING;"
        )

    for _, row in df.iterrows():
        name_text = cell_text(row["상품 이름"])
        if not name_text:
            continue
        category_text = cell_text(row["카테고리"], "기타")
        name, category = esc(name_text), esc(category_text)
        price = parse_price(row["가격"])
        img = image_url(row.get("이미지 파일"))
        link, coupang_key = canonical_purchase_url(row.get("링크"))
        link_sql = sql_text(link)
        img_sql = sql_text(img)
        seller_email = rng.choice(SELLER_EMAILS)
        duplicate_check = (
            purchase_url_duplicate_check(link_sql, coupang_key)
            if link_sql != "NULL"
            else f"name = '{name}' AND image_url IS NOT DISTINCT FROM {img_sql}"
        )

        stmts.append(
            "INSERT INTO products "
            "(seller_id,name,description,price,category,key_color,sub_color,image_url,purchase_url) "
            f"SELECT u.id,'{name}','{category} 상품',{price},'{category}',NULL,NULL,{img_sql},{link_sql} "
            f"FROM users u WHERE u.email = {sql_literal(seller_email)} "
            "AND NOT EXISTS ("
            f"SELECT 1 FROM products WHERE {duplicate_check}"
            ");"
        )
    image_prefix = like_esc(f"{PUBLIC_BASE.rstrip('/')}/")
    stmts.append(
        "UPDATE products SET key_color = NULL, sub_color = NULL "
        f"WHERE image_url LIKE '{image_prefix}%' ESCAPE '\\';"
    )
    stmts.append(
        "DELETE FROM product_colors pc USING products p "
        "WHERE pc.product_id = p.id "
        f"AND p.image_url LIKE '{image_prefix}%' ESCAPE '\\';"
    )
    stmts.append(
        "UPDATE products "
        "SET purchase_url = 'https://www.coupang.com/vp/products/' "
        "|| substring(purchase_url from '/vp/products/([^/?&]+)') "
        "|| '?itemId=' || substring(purchase_url from '[?&]itemId=([^&]+)') "
        "|| '&vendorItemId=' || substring(purchase_url from '[?&]vendorItemId=([^&]+)') "
        f"WHERE image_url LIKE '{image_prefix}%' ESCAPE '\\' "
        "AND substring(purchase_url from '/vp/products/([^/?&]+)') IS NOT NULL "
        "AND substring(purchase_url from '[?&]itemId=([^&]+)') IS NOT NULL "
        "AND substring(purchase_url from '[?&]vendorItemId=([^&]+)') IS NOT NULL;"
    )
    stmts.append(
        "WITH ranked_products AS ("
        "SELECT id, row_number() OVER ("
        "PARTITION BY substring(purchase_url from '/vp/products/([^/?&]+)'), "
        "substring(purchase_url from '[?&]itemId=([^&]+)'), "
        "substring(purchase_url from '[?&]vendorItemId=([^&]+)') "
        "ORDER BY id"
        ") AS rn "
        "FROM products "
        f"WHERE image_url LIKE '{image_prefix}%' ESCAPE '\\' "
        "AND substring(purchase_url from '/vp/products/([^/?&]+)') IS NOT NULL "
        "AND substring(purchase_url from '[?&]itemId=([^&]+)') IS NOT NULL "
        "AND substring(purchase_url from '[?&]vendorItemId=([^&]+)') IS NOT NULL"
        ") "
        "DELETE FROM products p USING ranked_products r "
        "WHERE p.id = r.id AND r.rn > 1;"
    )
    stmts.append("COMMIT;")
    run_psql("\n".join(stmts))
    print("done")

if __name__ == "__main__": main()
