package com.rag.ingestion.common.model;

/** Part of a page under one heading. {@code heading} is empty for the text before the first heading. */
public record Section(String heading, String text) {}
