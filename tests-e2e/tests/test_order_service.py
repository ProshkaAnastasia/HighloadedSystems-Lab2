# tests/test_order_service.py
import requests
import pytest

from fixtures_data import user, product_approved

BASE = "http://localhost:8080/order-service/api"


@pytest.mark.regression
class TestOrderService:

    def test_get_empty_cart(self, user):
        resp = requests.get(f"{BASE}/cart", params={"userId": user})
        assert resp.status_code == 200
        assert resp.json()["items"] == []

    def test_add_to_cart_and_get(self, user, product_approved):
        payload = {"productId": product_approved, "quantity": 2}
        resp = requests.post(
            f"{BASE}/cart/items",
            params={"userId": user},
            json=payload,
        )
        assert resp.status_code == 200
        cart = resp.json()
        assert len(cart["items"]) == 1

        resp2 = requests.get(f"{BASE}/cart", params={"userId": user})
        assert resp2.status_code == 200
        assert len(resp2.json()["items"]) == 1

    def test_update_cart_item(self, user, product_approved):
        payload = {"productId": product_approved, "quantity": 1}
        requests.post(f"{BASE}/cart/items", params={"userId": user}, json=payload)
        cart = requests.get(f"{BASE}/cart", params={"userId": user}).json()
        item_id = cart["items"][0]["id"]

        update_payload = {"quantity": 5}
        resp = requests.put(
            f"{BASE}/cart/items/{item_id}",
            params={"userId": user},
            json=update_payload,
        )
        assert resp.status_code == 200
        new_cart = resp.json()
        assert new_cart["items"][0]["quantity"] == 5

    def test_remove_from_cart(self, user, product_approved):
        payload = {"productId": product_approved, "quantity": 1}
        requests.post(f"{BASE}/cart/items", params={"userId": user}, json=payload)
        cart = requests.get(f"{BASE}/cart", params={"userId": user}).json()
        item_id = cart["items"][0]["id"]

        resp = requests.delete(
            f"{BASE}/cart/items/{item_id}",
            params={"userId": user},
        )
        assert resp.status_code == 200
        assert requests.get(f"{BASE}/cart", params={"userId": user}).json()["items"] == []

    def test_clear_cart(self, user, product_approved):
        payload = {"productId": product_approved, "quantity": 2}
        requests.post(f"{BASE}/cart/items", params={"userId": user}, json=payload)

        resp = requests.delete(f"{BASE}/cart", params={"userId": user})
        assert resp.status_code == 204
        assert requests.get(f"{BASE}/cart", params={"userId": user}).json()["items"] == []

    def test_create_order_from_cart(self, user, product_approved):
        payload = {"productId": product_approved, "quantity": 2}
        requests.post(f"{BASE}/cart/items", params={"userId": user}, json=payload)

        order_payload = {"deliveryAddress": "SPb, Nevsky 1"}
        resp = requests.post(
            f"{BASE}/orders",
            params={"userId": user},
            json=order_payload,
        )
        assert resp.status_code == 201
        order = resp.json()
        assert order["userId"] == user
        assert len(order["items"]) == 1

    def test_get_user_orders_and_order_by_id(self, user, product_approved):
        payload = {"productId": product_approved, "quantity": 1}
        requests.post(f"{BASE}/cart/items", params={"userId": user}, json=payload)
        order_resp = requests.post(
            f"{BASE}/orders",
            params={"userId": user},
            json={"deliveryAddress": "Test"},
        )
        order_id = order_resp.json()["id"]

        list_resp = requests.get(
            f"{BASE}/orders", params={"userId": user, "page": 1, "pageSize": 20}
        )
        assert list_resp.status_code == 200
        page = list_resp.json()
        assert any(o["id"] == order_id for o in page["data"])

        by_id = requests.get(
            f"{BASE}/orders/{order_id}", params={"userId": user}
        )
        assert by_id.status_code == 200
        assert by_id.json()["id"] == order_id
