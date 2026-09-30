package com.rag.ingestion.service.model;

public record SynchronizationSummary(int newPages, int updatedPages, int unchangedPages, int chunks) {}
