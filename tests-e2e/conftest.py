import subprocess
from contextlib import contextmanager
from typing import Iterator

import psycopg2
import pytest
import requests
from tenacity import retry, stop_after_delay, wait_fixed

GATEWAY_URL = "http://localhost:8080"
COMPOSE_FILE = "docker-compose.yml"


# ---------- Docker-compose lifecycle ----------

@pytest.fixture(scope="session", autouse=True)
def docker_compose() -> Iterator[None]:
    """Поднимает весь стек docker-compose и гасит его после тестов."""
    # Остановить и очистить старое
    subprocess.run(
        ["docker-compose", "-f", COMPOSE_FILE, "down", "-v"],
        check=False,
        capture_output=True,
    )

    # Поднять всё
    subprocess.run(
        ["docker-compose", "-f", COMPOSE_FILE, "up", "-d", "--build"],
        check=True,
    )

    yield

    # Остановить после тестов
    subprocess.run(
        ["docker-compose", "-f", COMPOSE_FILE, "down", "-v"],
        check=False,
    )


@retry(stop=stop_after_delay(240), wait=wait_fixed(5))
def _wait_gateway_health() -> None:
    """Ждём, пока gateway начнёт отвечать по /actuator/health."""
    resp = requests.get(f"{GATEWAY_URL}/actuator/health", timeout=10)
    resp.raise_for_status()


@retry(stop=stop_after_delay(180), wait=wait_fixed(5))
def _wait_eureka() -> None:
    """Ждём UI Eureka, чтобы быть уверенными, что сервисы зарегистрированы."""
    resp = requests.get("http://localhost:8761/", timeout=10)
    resp.raise_for_status()


@pytest.fixture(scope="session")
def infrastructure_ready(docker_compose) -> str:
    """Инфраструктура готова: gateway + eureka."""
    _wait_gateway_health()
    _wait_eureka()
    return GATEWAY_URL


# ---------- PostgreSQL connections ----------

@contextmanager
def pg_connection(host: str, port: int, db: str, user: str = "itmouser", password: str = "itmopassword"):
    conn = psycopg2.connect(
        host=host,
        port=port,
        dbname=db,
        user=user,
        password=password,
    )
    try:
        yield conn
    finally:
        conn.close()


@pytest.fixture(scope="session")
def db_user(infrastructure_ready):
    """БД user-service: postgres-user:5401 → itmomarket_user."""
    with pg_connection("localhost", 5401, "itmomarket_user") as conn:
        yield conn


@pytest.fixture(scope="session")
def db_product(infrastructure_ready):
    """БД product-service: postgres-product:5402 → itmomarket_product."""
    with pg_connection("localhost", 5402, "itmomarket_product") as conn:
        yield conn


@pytest.fixture(scope="session")
def db_order(infrastructure_ready):
    """БД order-service: postgres-order:5403 → itmomarket_order."""
    with pg_connection("localhost", 5403, "itmomarket_order") as conn:
        yield conn


@pytest.fixture(scope="session")
def db_moderation(infrastructure_ready):
    """БД moderation-service: postgres-moderation:5404 → itmomarket_moderation."""
    with pg_connection("localhost", 5404, "itmomarket_moderation") as conn:
        yield conn


# ---------- Очистка БД между тестами ----------

@pytest.fixture(autouse=True)
def clean_databases(db_user, db_product, db_order, db_moderation):
    """
    Очищает все БД перед каждым тестом.
    ВАЖНО: порядок TRUNCATE подогнать под реальные имена таблиц и FK.
    """
    # Порядок: сначала таблицы без FK, потом с FK
    truncate_queries = [
        "TRUNCATE TABLE order_items CASCADE;",
        "TRUNCATE TABLE orders CASCADE;",
        "TRUNCATE TABLE carts CASCADE;",
        "TRUNCATE TABLE moderation_audit CASCADE;",  # ← FIX: moderation_audit (не audits)
        "TRUNCATE TABLE moderation_actions CASCADE;",
        "TRUNCATE TABLE products CASCADE;",
        "TRUNCATE TABLE shops CASCADE;",
        "TRUNCATE TABLE user_roles CASCADE;",  # ← FIX: добавили user_roles
        "TRUNCATE TABLE users CASCADE;",
    ]

    for conn_name, conn in [("db_order", db_order), ("db_moderation", db_moderation), 
                             ("db_product", db_product), ("db_user", db_user)]:
        try:
            with conn.cursor() as cur:
                for q in truncate_queries:
                    try:
                        cur.execute(q)
                        conn.commit()  # ← FIX: коммит каждого TRUNCATE отдельно
                    except psycopg2.errors.UndefinedTable:
                        # Таблица не существует в этой БД — пропускаем
                        conn.rollback()
                    except Exception as e:
                        # Другие ошибки логируем и откатываем
                        conn.rollback()
                        print(f"Warning in {conn_name}: {e}")
        except Exception as e:
            print(f"Error cleaning {conn_name}: {e}")

    yield