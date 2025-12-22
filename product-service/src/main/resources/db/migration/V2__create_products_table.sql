CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(19, 2) NOT NULL,
    image_url VARCHAR(500),
    shop_id BIGINT NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    seller_id BIGINT NOT NULL,  -- ← Убрали REFERENCES users(id)
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT products_price_positive CHECK (price > 0)
);

CREATE INDEX idx_products_shop_id ON products(shop_id);
CREATE INDEX idx_products_seller_id ON products(seller_id);
CREATE INDEX idx_products_status ON products(status);
