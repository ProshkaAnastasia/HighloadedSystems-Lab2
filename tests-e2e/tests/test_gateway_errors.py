# tests/test_gateway_errors.py
import requests
import pytest


BASE = "http://localhost:8080"


@pytest.mark.error_handling
def test_not_found_endpoint(infrastructure_ready):
    resp = requests.get(f"{BASE}/non-existent-path")
    assert resp.status_code in (404, 500)


@pytest.mark.error_handling
def test_invalid_product_id(infrastructure_ready):
    resp = requests.get(f"{BASE}/product-service/api/products/-1")
    assert resp.status_code in (400, 404)


@pytest.mark.error_handling
def test_invalid_cart_user_id(infrastructure_ready):
    resp = requests.get(
        f"{BASE}/order-service/api/cart",
        params={"userId": -1},
    )
    assert resp.status_code in (400, 404)
