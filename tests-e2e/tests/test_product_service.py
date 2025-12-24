# tests/test_product_service.py
import requests
import pytest

from fixtures_data import seller, shop, product_approved

BASE = "http://localhost:8080/product-service/api"


@pytest.mark.regression
class TestProductService:

    def test_create_shop(self, seller):
        payload = {
            "name": "Py Shop",
            "description": "Created via tests",
            "avatarUrl": None,
        }
        resp = requests.post(
            f"{BASE}/shops",
            params={"sellerId": seller},
            json=payload,
        )
        assert resp.status_code == 201
        assert resp.json()["sellerId"] == seller

    def test_get_shop_by_id(self, shop):
        resp = requests.get(f"{BASE}/shops/{shop}")
        assert resp.status_code == 200
        assert resp.json()["id"] == shop

    def test_get_all_shops(self):
        resp = requests.get(f"{BASE}/shops", params={"page": 1, "pageSize": 20})
        assert resp.status_code == 200
        data = resp.json()
        assert "data" in data

    def test_create_product(self, seller, shop):
        payload = {
            "name": "E2E Product",
            "description": "From tests",
            "price": 199.99,
            "imageUrl": None,
            "shopId": shop,
        }
        resp = requests.post(
            f"{BASE}/products",
            params={"sellerId": seller},
            json=payload,
        )
        assert resp.status_code == 201
        product = resp.json()
        assert product["name"] == "E2E Product"

    def test_get_product_by_id(self, product_approved):
        resp = requests.get(f"{BASE}/products/{product_approved}")
        assert resp.status_code == 200
        assert resp.json()["id"] == product_approved

    def test_get_all_products(self, product_approved):
        resp = requests.get(f"{BASE}/products", params={"page": 1, "pageSize": 20})
        assert resp.status_code == 200
        page = resp.json()
        ids = [p["id"] for p in page["data"]]
        assert product_approved in ids

    def test_search_products(self, product_approved):
        resp = requests.get(
            f"{BASE}/products/search",
            params={"keywords": "Approved", "page": 1, "pageSize": 20},
        )
        assert resp.status_code == 200
        assert len(resp.json()["data"]) >= 1

    def test_get_pending_products(self):
        resp = requests.get(
            f"{BASE}/products/pending",
            params={"page": 1, "pageSize": 20},
        )
        assert resp.status_code == 200

    def test_get_shop_products(self, shop, product_approved):
        resp = requests.get(
            f"{BASE}/shops/{shop}/products",
            params={"page": 1, "pageSize": 20},
        )
        assert resp.status_code == 200

    def test_update_product(self, seller, moderator, shop):
        create_resp = requests.post(
            f"{BASE}/products",
            params={"sellerId": seller},
            json={"name": "Old", "price": 10.0, "shopId": shop},
        )
        product_id = create_resp.json()["id"]

        update_payload = {
            "name": "Updated name",
            "description": "Updated desc",
            "price": 20.0,
            "imageUrl": None,
        }
        resp = requests.put(
            f"{BASE}/products/{product_id}",
            params={"userId": moderator},
            json=update_payload,
        )
        assert resp.status_code == 200
        assert resp.json()["name"] == "Updated name"

    def test_delete_product(self, seller, shop):
        create_resp = requests.post(
            f"{BASE}/products",
            params={"sellerId": seller},
            json={"name": "ToDelete", "price": 10.0, "shopId": shop},
        )
        product_id = create_resp.json()["id"]

        resp = requests.delete(
            f"{BASE}/products/{product_id}",
            params={"userId": seller},
        )
        assert resp.status_code == 204

        resp2 = requests.get(f"{BASE}/products/{product_id}")
        assert resp2.status_code == 404
