package com.rag.ingestion.controller.dto;

public record SynchronizationResult(int newPages, int updatedPages, int unchangedPages, int chunks) {}
