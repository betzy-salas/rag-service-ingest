package com.rag.ingestion.client.dto;

/** Response links: {@code next} and {@code base} in listings, {@code webui} in each page. */
public record Links(String next, String base, String webui) {}
