#!/usr/bin/env python3
"""Import is_use=true products from coupang_products.xlsx into PostgreSQL."""
from __future__ import annotations
import re, subprocess, sys
from pathlib import Path
import pandas as pd

ROOT = Path(__file__).resolve().parents[1]
XLSX = ROOT / "data" / "coupang_products.xlsx"
if not XLSX.exists():
    XLSX = ROOT / "coupang_products.xlsx"  # fallback: 루트에 파일이 있는 경우
PUBLIC_BASE = sys.argv[1] if len(sys.argv) > 1 else "https://hakku.rearleg.com/product-images"
SELLER_EMAIL = "seller@hakku.local"
SELLER_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
COLOR_FIX = {"yeloow": "yellow"}

def normalize_color(value):
    if value is None or (isinstance(value, float) and pd.isna(value)): return None
    text = str(value).strip().lower()
    return COLOR_FIX.get(text, text) if text else None

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

def run_psql(sql: str) -> None:
    subprocess.run(["docker","exec","-i","hakku-postgres","psql","-U","hakku","-d","hakku"], input=sql, text=True, check=True)

def main():
    df = pd.read_excel(XLSX)
    used = df[df["is_use"] == True]
    print(f"Importing {len(used)} products")
    stmts = [
        f"INSERT INTO users (email,nickname,password_hash,role,diagnosis_status) VALUES ('{SELLER_EMAIL}','학꾸 공식','{SELLER_HASH}','SELLER','NONE') ON CONFLICT (email) DO NOTHING;",
        "DELETE FROM product_colors;", "DELETE FROM product_styles;", "DELETE FROM products;",
    ]
    for _, row in used.iterrows():
        name, category = esc(str(row["상품 이름"]).strip()), esc(str(row["카테고리"]).strip())
        price = parse_price(row["가격"])
        img = image_url(row.get("이미지 파일"))
        link = row.get("링크")
        link_sql = "NULL"
        if link is not None and not (isinstance(link, float) and pd.isna(link)):
            link_text = str(link).strip()
            if link_text:
                link_sql = f"'{esc(link_text)}'"
        img_sql = f"'{esc(img)}'" if img else "NULL"
        colors = []
        for i in range(1, 7):
            c = normalize_color(row.get(f"color_{i}"))
            if c and c not in colors: colors.append(c)
        key = "NULL" if not colors or colors[0].upper() == "ALL" else f"'{esc(colors[0])}'"
        sub = f"'{esc(colors[1])}'" if len(colors) > 1 and colors[1].upper() != "ALL" else "NULL"
        stmts.append(f"INSERT INTO products (seller_id,name,description,price,category,key_color,sub_color,image_url,purchase_url) SELECT u.id,'{name}','{category} 상품',{price},'{category}',{key},{sub},{img_sql},{link_sql} FROM users u WHERE u.email='{SELLER_EMAIL}';")
        for color in colors:
            stmts.append(f"INSERT INTO product_colors (product_id,color) SELECT p.id,'{esc(color)}' FROM products p JOIN users u ON p.seller_id=u.id WHERE u.email='{SELLER_EMAIL}' AND p.name='{name}' ON CONFLICT DO NOTHING;")
    run_psql("\n".join(stmts))
    print("done")

if __name__ == "__main__": main()
