"""Unit tests for seed_sellers pure helpers (no DB required).

Run from the scripts/ directory:
    python3 -m unittest test_seed_sellers
"""
import unittest

import seed_sellers


class SellerRowsTest(unittest.TestCase):
    def test_returns_requested_count(self):
        rows = seed_sellers.seller_rows(20)
        self.assertEqual(len(rows), 20)

    def test_emails_are_unique_and_zero_padded(self):
        rows = seed_sellers.seller_rows(20)
        emails = [r["email"] for r in rows]
        self.assertEqual(len(set(emails)), 20)
        self.assertEqual(emails[0], "seller01@hakku.local")
        self.assertEqual(emails[19], "seller20@hakku.local")

    def test_every_row_has_a_nickname(self):
        rows = seed_sellers.seller_rows(5)
        self.assertTrue(all(r["nickname"].strip() for r in rows))


class BuildSeedSqlTest(unittest.TestCase):
    def test_inserts_seller_role_idempotently(self):
        rows = seed_sellers.seller_rows(3)
        sql = seed_sellers.build_seed_sql(rows, "$2a$10$hash")
        self.assertIn("INSERT INTO users", sql)
        self.assertIn("'SELLER'", sql)
        self.assertIn("ON CONFLICT (email) DO NOTHING", sql)
        # every seller email must appear in the generated statement
        for row in rows:
            self.assertIn(row["email"], sql)
        # password hash is included for every row
        self.assertEqual(sql.count("$2a$10$hash"), len(rows))

    def test_escapes_single_quotes_in_values(self):
        sql = seed_sellers.build_seed_sql(
            [{"email": "x@hakku.local", "nickname": "O'Brien"}], "$2a$10$hash"
        )
        self.assertIn("O''Brien", sql)


if __name__ == "__main__":
    unittest.main()
