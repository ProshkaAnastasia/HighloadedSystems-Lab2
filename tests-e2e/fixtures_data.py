# fixtures_data.py
from typing import List
import uuid

import psycopg2
import pytest


# ---------- низкоуровневые функции вставки ----------


def create_user(conn: psycopg2.extensions.connection,
                username: str,
                email: str,
                roles: List[str]) -> int:
    """
    Создаёт пользователя в БД user-service и его роли.
    Подстрой имена таблиц/колонок под свою схему.
    """
    try:
        with conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO users (username, email, password, first_name, last_name, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, NOW(), NOW())
                RETURNING id;
                """,
                (username, email, "$2a$10$e2ehashedpassword", "Test", "User"),
            )
            user_id = cur.fetchone()[0]

            # если есть таблица user_roles
            for role in roles:
                try:
                    cur.execute(
                        """
                        INSERT INTO user_roles (user_id, role)
                        VALUES (%s, %s);
                        """,
                        (user_id, role),
                    )
                except psycopg2.errors.UndefinedTable:
                    # Таблица не существует, просто пропускаем
                    conn.rollback()
                    break

        conn.commit()
        return user_id
    except Exception as e:
        conn.rollback()  # ← Откати транзакцию при ошибке
        raise


def create_shop(conn: psycopg2.extensions.connection,
                name: str,
                seller_id: int) -> int:
    """
    Создаёт магазин в БД product-service.
    """
    try:
        with conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO shops (name, description, avatar_url, seller_id, created_at, updated_at)
                VALUES (%s, %s, %s, %s, NOW(), NOW())
                RETURNING id;
                """,
                (name, "Test shop", None, seller_id),
            )
            shop_id = cur.fetchone()[0]
        conn.commit()
        return shop_id
    except Exception as e:
        conn.rollback()
        raise


def create_product(conn: psycopg2.extensions.connection,
                   name: str,
                   shop_id: int,
                   seller_id: int,
                   status: str) -> int:
    """
    Создаёт товар в БД product-service.
    """
    try:
        with conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO products (name, description, price, image_url, shop_id, seller_id, status, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
                RETURNING id;
                """,
                (name, "Test product", 100.00, None, shop_id, seller_id, status),
            )
            product_id = cur.fetchone()[0]
        conn.commit()
        return product_id
    except Exception as e:
        conn.rollback()
        raise


# ---------- pytest fixtures ----------


@pytest.fixture
def user(db_user) -> int:
    """Обычный пользователь с ролью USER."""
    unique_id = str(uuid.uuid4())[:8]
    return create_user(
        db_user,
        f"e2e_user_{unique_id}",
        f"e2e_user_{unique_id}@example.com",
        ["USER"]
    )


@pytest.fixture
def seller(db_user) -> int:
    """Продавец с ролью SELLER."""
    unique_id = str(uuid.uuid4())[:8]
    return create_user(
        db_user,
        f"e2e_seller_{unique_id}",
        f"e2e_seller_{unique_id}@example.com",
        ["SELLER"]
    )


@pytest.fixture
def moderator(db_user) -> int:
    """Модератор с ролью MODERATOR."""
    unique_id = str(uuid.uuid4())[:8]
    return create_user(
        db_user,
        f"e2e_moderator_{unique_id}",
        f"e2e_moderator_{unique_id}@example.com",
        ["MODERATOR"]
    )


@pytest.fixture
def shop(db_product, seller) -> int:
    """Магазин для продавца."""
    unique_id = str(uuid.uuid4())[:8]
    return create_shop(
        db_product,
        f"E2E Shop {unique_id}",
        seller
    )


@pytest.fixture
def product_pending(db_product, seller, shop) -> int:
    """Товар в статусе PENDING (для модерации)."""
    unique_id = str(uuid.uuid4())[:8]
    return create_product(
        db_product,
        f"Pending Product {unique_id}",
        shop,
        seller,
        "PENDING"
    )


@pytest.fixture
def product_approved(db_product, seller, shop) -> int:
    """Товар в статусе APPROVED (для каталога и корзины)."""
    unique_id = str(uuid.uuid4())[:8]
    return create_product(
        db_product,
        f"Approved Product {unique_id}",
        shop,
        seller,
        "APPROVED"
    )
