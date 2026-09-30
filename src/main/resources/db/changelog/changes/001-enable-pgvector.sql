--liquibase formatted sql

--changeset confluence-ingestion:001-enable-pgvector
CREATE EXTENSION IF NOT EXISTS vector;
--rollback DROP EXTENSION IF EXISTS vector;
