# tests/test_user_service.py
import requests
import pytest

from fixtures_data import user

BASE = "http://localhost:8080/user-service/api/users"


@pytest.mark.regression
class TestUserService:

    def test_register_user(self, infrastructure_ready):
        payload = {
            "username": "newuser",
            "email": "new@example.com",
            "password": "Test12345!",
            "firstName": "New",
            "lastName": "User",
        }
        resp = requests.post(f"{BASE}/register", json=payload)
        assert resp.status_code == 201
        data = resp.json()
        assert data["username"] == "newuser"

    def test_get_user_by_id(self, infrastructure_ready, user):
        resp = requests.get(f"{BASE}/{user}")
        assert resp.status_code == 200
        data = resp.json()
        assert data["id"] == user

    def test_get_user_by_username(self, infrastructure_ready, user):
        resp = requests.get(f"{BASE}/username/e2e_user")
        assert resp.status_code == 200
        data = resp.json()
        assert data["username"] == "e2e_user"

    def test_get_me(self, infrastructure_ready, user):
        resp = requests.get(f"{BASE}/me", params={"userId": user})
        assert resp.status_code == 200
        assert resp.json()["id"] == user

    def test_update_profile(self, infrastructure_ready, user):
        payload = {
            "email": "updated@example.com",
            "firstName": "Updated",
            "lastName": "User",
        }
        resp = requests.put(f"{BASE}/me", params={"userId": user}, json=payload)
        assert resp.status_code == 200
        data = resp.json()
        assert data["email"] == "updated@example.com"

    def test_delete_user(self, infrastructure_ready, user):
        resp = requests.delete(f"{BASE}/{user}")
        assert resp.status_code == 204
        resp2 = requests.get(f"{BASE}/{user}")
        assert resp2.status_code == 404
