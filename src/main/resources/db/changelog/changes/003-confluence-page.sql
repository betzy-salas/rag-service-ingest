--liquibase formatted sql

--changeset confluence-ingestion:003-confluence-page
-- One row per indexed page. Its version is the one stored in confluence_chunk and drives the
-- synchronization. There is no status column: a page is either indexed (it has a row) or not.
CREATE TABLE confluence_page (
    page_id     VARCHAR(64)   PRIMARY KEY,
    space_key   VARCHAR(64)   NOT NULL,
    title       VARCHAR(1000) NOT NULL,
    version     INT           NOT NULL,
    chunk_count INT           NOT NULL,
    indexed_at  TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_confluence_page_space_key ON confluence_page (space_key);
--rollback DROP TABLE confluence_page;
