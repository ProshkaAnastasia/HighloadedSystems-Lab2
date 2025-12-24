# tests/test_e2e_happy_path.py
import requests
import pytest

from fixtures_data import user, seller, shop

BASE = "http://localhost:8080"


@pytest.mark.smoke
def test_full_shopping_flow(user, seller, moderator, shop):
    # 1. Создаём товар
    create_resp = requests.post(
        f"{BASE}/product-service/api/products",
        params={"sellerId": seller},
        json={
            "name": "Flow Product",
            "description": "Full flow",
            "price": 50.0,
            "imageUrl": None,
            "shopId": shop,
        },
    )
    assert create_resp.status_code == 201
    product_id = create_resp.json()["id"]

    # 2. Модератор одобряет
    approve_resp = requests.post(
        f"{BASE}/moderation-service/api/moderation/products/{product_id}/approve",
        params={"moderatorId": moderator},
    )
    assert approve_resp.status_code == 200

    # 3. Пользователь добавляет в корзину
    add_resp = requests.post(
        f"{BASE}/order-service/api/cart/items",
        params={"userId": user},
        json={"productId": product_id, "quantity": 2},
    )
    assert add_resp.status_code == 200

    # 4. Создаём заказ
    order_resp = requests.post(
        f"{BASE}/order-service/api/orders",
        params={"userId": user},
        json={"deliveryAddress": "SPb, Nevsky 1"},
    )
    assert order_resp.status_code == 201
    order = order_resp.json()
    assert order["userId"] == user
    assert len(order["items"]) == 1
