--liquibase formatted sql

--changeset confluence-ingestion:002-confluence-chunk
-- Chunks of Confluence pages with their embeddings. Same columns as the default Spring AI
-- PgVectorStore table, so any Spring AI client can read it by setting
-- spring.ai.vectorstore.pgvector.table-name.
-- Metadata is JSONB with a GIN index to delete and filter chunks by pageId or spaceKey.
-- No HNSW/IVFFlat index: pgvector cannot index vectors with more than 2000 dimensions.
CREATE TABLE confluence_chunk (
    id        UUID         PRIMARY KEY,
    content   TEXT         NOT NULL,
    metadata  JSONB        NOT NULL,
    embedding VECTOR(3072) NOT NULL
);
CREATE INDEX idx_confluence_chunk_metadata ON confluence_chunk USING GIN (metadata jsonb_path_ops);
--rollback DROP TABLE confluence_chunk;
