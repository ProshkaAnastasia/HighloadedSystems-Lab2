import subprocess
from contextlib import contextmanager
from typing import Iterator

import psycopg2
import pytest
import requests
from tenacity import retry, stop_after_delay, wait_fixed

pytest_plugins = ["fixtures_data"]

GATEWAY_URL = "http://localhost:8080"
COMPOSE_FILE = "docker-compose.yml"
EUREKA_URL = "http://localhost:8761"


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


@retry(stop=stop_after_delay(500), wait=wait_fixed(5))
def _wait_gateway_health() -> None:
    """Ждём, пока gateway начнёт отвечать по /actuator/health."""
    resp = requests.get(f"{GATEWAY_URL}/actuator/health", timeout=10)
    resp.raise_for_status()


@retry(stop=stop_after_delay(500), wait=wait_fixed(5))
def _wait_eureka() -> None:
    """Ждём UI Eureka, чтобы быть уверенными, что сервисы зарегистрированы."""
    resp = requests.get("http://localhost:8761/", timeout=10)
    resp.raise_for_status()


@retry(stop=stop_after_delay(500), wait=wait_fixed(5))
def _wait_service_healthy(service_name: str, service_id: str) -> None:
    """
    Проверяет здоровье сервиса через Gateway.
    
    Gateway маршрутизирует: localhost:8080/{service_id}/actuator/health
    """
    try:
        url = f"{GATEWAY_URL}/{service_id}/actuator/health"
        resp = requests.get(url, timeout=10)
        
        if resp.status_code == 503:
            print(f"[...] {service_name}: 503 Service Unavailable")
            raise Exception(f"{service_name} unavailable (503)")
        
        if resp.status_code != 200:
            print(f"[...] {service_name}: {resp.status_code}")
            raise Exception(f"{service_name} returned {resp.status_code}")
        
        health_data = resp.json()
        status = health_data.get("status", "UNKNOWN")
        
        print(f"[✓] {service_name} is UP (status: {status})")
        
    except requests.RequestException as e:
        print(f"[...] {service_name}: {type(e).__name__}")
        raise


@pytest.fixture(scope="session", autouse=True)
def infrastructure_ready(docker_compose) -> str:
    """
    Инфраструктура готова: gateway + eureka + все микросервисы.
    """
    print("\n" + "="*70)
    print("WAITING FOR INFRASTRUCTURE...")
    print("="*70)
    
    print("\n[1/6] Waiting for Gateway...")
    _wait_gateway_health()
    
    print("\n[2/6] Waiting for Eureka...")
    _wait_eureka()
    
    services = [
        ("User Service", "user-service"),
        ("Product Service", "product-service"),
        ("Order Service", "order-service"),
        ("Moderation Service", "moderation-service"),
    ]
    
    for idx, (service_name, service_id) in enumerate(services, start=3):
        print(f"\n[{idx}/6] Waiting for {service_name}...")
        _wait_service_healthy(service_name, service_id)
    
    print("\n" + "="*70)
    print("[✓] ALL INFRASTRUCTURE READY!")
    print("="*70 + "\n")
    
    return GATEWAY_URL


# ============================================================================
# PYTEST HOOKS
# ============================================================================

def pytest_configure(config):
    """
    Pre-test setup hook.
    Runs BEFORE test collection.
    Starts Docker infrastructure and waits for all services.
    """
    
    _print_header("PRE-TEST SETUP: Starting infrastructure")
    
    # Stop any existing containers
    print("Stopping old containers...")
    subprocess.run(
        ["docker-compose", "-f", COMPOSE_FILE, "down", "-v"],
        check=False,
        capture_output=True,
    )
    
    # Start new containers
    print("Starting docker-compose with --build...")
    result = subprocess.run(
        ["docker-compose", "-f", COMPOSE_FILE, "up", "-d", "--build"],
        capture_output=True,
        text=True,
    )
    
    if result.returncode != 0:
        _print_error(f"docker-compose failed: {result.stderr}")
        raise SystemExit(1)
    
    _print_success("Docker-compose started")
    
    # Wait for infrastructure
    _print_header("WAITING FOR SERVICES TO BE READY")
    
    try:
        _print_step(1, 6, "Waiting for API Gateway")
        _check_gateway()
        
        _print_step(2, 6, "Waiting for Service Registry (Eureka)")
        _check_eureka()
        
        # Wait for each microservice
        for idx, (service_name, service_id) in enumerate(SERVICES, start=3):
            _print_step(idx, 6, f"Waiting for {service_name}")
            _check_service(service_name, service_id)
        
        _print_header("[✓] ALL INFRASTRUCTURE READY!")
        print()
        
    except Exception as e:
        _print_error(f"Infrastructure setup failed: {e}")
        raise SystemExit(1)


def pytest_sessionfinish(session, exitstatus):
    """Post-test cleanup hook. Runs AFTER all tests."""
    _print_header("TEARDOWN: Stopping infrastructure")
    
    subprocess.run(
        ["docker-compose", "-f", COMPOSE_FILE, "down", "-v"],
        check=False,
        capture_output=True,
    )
    
    _print_success("Docker-compose stopped")
    print()


# ============================================================================
# DATABASE CONNECTIONS
# ============================================================================

@contextmanager
def pg_connection(
    host: str,
    port: int,
    db: str,
    user: str = "itmouser",
    password: str = "itmopassword"
):
    """Context manager for PostgreSQL connections."""
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


# ============================================================================
# SESSION-SCOPED FIXTURES
# ============================================================================

@pytest.fixture(scope="session")
def db_user():
    """User Service Database (postgres-user:5401)"""
    with pg_connection("localhost", 5401, "itmomarket_user") as conn:
        yield conn


@pytest.fixture(scope="session")
def db_product():
    """Product Service Database (postgres-product:5402)"""
    with pg_connection("localhost", 5402, "itmomarket_product") as conn:
        yield conn


@pytest.fixture(scope="session")
def db_order():
    """Order Service Database (postgres-order:5403)"""
    with pg_connection("localhost", 5403, "itmomarket_order") as conn:
        yield conn


@pytest.fixture(scope="session")
def db_moderation():
    """Moderation Service Database (postgres-moderation:5404)"""
    with pg_connection("localhost", 5404, "itmomarket_moderation") as conn:
        yield conn


# ============================================================================
# FUNCTION-SCOPED FIXTURES
# ============================================================================


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