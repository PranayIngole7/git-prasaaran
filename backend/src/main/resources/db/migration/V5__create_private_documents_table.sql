CREATE TABLE private_documents (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_private_documents_owner
        FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_private_documents_owner_slug UNIQUE (owner_id, slug)
);

CREATE INDEX idx_private_documents_owner_updated_at
    ON private_documents (owner_id, updated_at DESC);
