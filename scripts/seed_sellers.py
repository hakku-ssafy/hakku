#!/usr/bin/env python3
"""Seed ~20 SELLER accounts into PostgreSQL (idempotent).

Mirrors import_products.py: talks to the local `hakku-postgres` container via
docker exec. Re-runnable — existing emails are skipped (ON CONFLICT DO NOTHING).

Usage:
    python3 seed_sellers.py [count]      # default 20

All seeded sellers share the demo password "password" (Spring's canonical
BCrypt sample hash), so you can log in as e.g. seller01@hakku.local / password.
"""
from __future__ import annotations

import subprocess
import sys

# BCrypt hash of the literal password "password" (Spring Security's sample hash;
# also used by import_products.py). Valid for org.springframework BCryptPasswordEncoder.
SELLER_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
DEFAULT_COUNT = 20


def esc(value: str) -> str:
    """Escape single quotes for safe inlining into a SQL string literal."""
    return value.replace("'", "''")


def seller_rows(count: int) -> list[dict[str, str]]:
    """Build `count` demo seller records with stable, zero-padded identities."""
    rows: list[dict[str, str]] = []
    for i in range(1, count + 1):
        idx = f"{i:02d}"
        rows.append({"email": f"seller{idx}@hakku.local", "nickname": f"학꾸 판매자 {idx}"})
    return rows


def build_seed_sql(rows: list[dict[str, str]], password_hash: str) -> str:
    """Render a single idempotent multi-row INSERT for the given seller rows."""
    values = ",\n".join(
        "('{email}','{nickname}','{hash}','SELLER','NONE',true)".format(
            email=esc(row["email"]), nickname=esc(row["nickname"]), hash=esc(password_hash)
        )
        for row in rows
    )
    return (
        "INSERT INTO users (email, nickname, password_hash, role, "
        "diagnosis_status, onboarding_completed) VALUES\n"
        f"{values}\n"
        "ON CONFLICT (email) DO NOTHING;"
    )


def run_psql(sql: str) -> None:
    subprocess.run(
        ["docker", "exec", "-i", "hakku-postgres", "psql", "-U", "hakku", "-d", "hakku"],
        input=sql,
        text=True,
        check=True,
    )


def main() -> None:
    count = int(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_COUNT
    rows = seller_rows(count)
    print(f"Seeding {len(rows)} sellers (seller01..seller{count:02d}@hakku.local, password: password)")
    run_psql(build_seed_sql(rows, SELLER_PASSWORD_HASH))
    print("done")


if __name__ == "__main__":
    main()
