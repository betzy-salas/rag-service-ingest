package com.rag.ingestion.client.dto;

public record Page(String id, String title, Version version, Body body, Links _links) {}
