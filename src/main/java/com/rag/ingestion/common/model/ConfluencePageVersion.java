package com.rag.ingestion.common.model;

/** Page from the lightweight listing, without body: enough to detect whether it changed. */
public record ConfluencePageVersion(String id, String title, int version) {}
