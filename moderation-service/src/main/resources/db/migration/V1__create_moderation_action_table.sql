CREATE TABLE IF NOT EXISTS moderation_actions (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    moderator_id BIGINT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_product_moderation UNIQUE(product_id, action_type)
);

CREATE INDEX IF NOT EXISTS idx_moderation_actions_product_id ON moderation_actions(product_id);
CREATE INDEX IF NOT EXISTS idx_moderation_actions_moderator_id ON moderation_actions(moderator_id);
CREATE INDEX IF NOT EXISTS idx_moderation_actions_action_type ON moderation_actions(action_type);
CREATE INDEX IF NOT EXISTS idx_moderation_actions_created_at ON moderation_actions(created_at);
