# tests/test_moderation_service.py
import requests
import pytest

from fixtures_data import moderator, product_pending, seller, shop, create_product

BASE = "http://localhost:8080/moderation-service/api/moderation"


@pytest.mark.regression
class TestModerationService:

    def test_get_pending_products(self, moderator, product_pending):
        resp = requests.get(
            f"{BASE}/products",
            params={"moderatorId": moderator, "page": 1, "pageSize": 20},
        )
        assert resp.status_code == 200

    def test_get_pending_product_by_id(self, moderator, product_pending):
        resp = requests.get(
            f"{BASE}/products/{product_pending}",
            params={"moderatorId": moderator},
        )
        assert resp.status_code == 200
        assert resp.json()["id"] == product_pending

    def test_approve_product(self, moderator, product_pending):
        resp = requests.post(
            f"{BASE}/products/{product_pending}/approve",
            params={"moderatorId": moderator},
        )
        assert resp.status_code == 200
        result = resp.json()
        assert result["productId"] == product_pending
        assert result["newStatus"] == "APPROVED"

    def test_reject_product(self, moderator, product_pending):
        payload = {"reason": "Invalid description"}
        resp = requests.post(
            f"{BASE}/products/{product_pending}/reject",
            params={"moderatorId": moderator},
            json=payload,
        )
        assert resp.status_code == 200
        result = resp.json()
        assert result["newStatus"] == "REJECTED"
        assert result["reason"] == "Invalid description"

    def test_bulk_moderate(self, moderator, db_product, seller, shop):
        ids = [
            create_product(db_product, f"Bulk {i}", shop, seller, "PENDING")
            for i in range(3)
        ]
        payload = {"productIds": ids, "action": "APPROVE"}
        resp = requests.post(
            f"{BASE}/bulk",
            params={"moderatorId": moderator},
            json=payload,
        )
        assert resp.status_code == 200
        results = resp.json()
        assert len(results) == 3
        assert all(r["newStatus"] == "APPROVED" for r in results)

    def test_moderation_history_by_moderator(self, moderator):
        resp = requests.get(
            f"{BASE}/history",
            params={"moderatorId": moderator},
        )
        assert resp.status_code == 200

    def test_product_moderation_history(self, product_pending):
        resp = requests.get(f"{BASE}/products/{product_pending}/history")
        assert resp.status_code == 200
