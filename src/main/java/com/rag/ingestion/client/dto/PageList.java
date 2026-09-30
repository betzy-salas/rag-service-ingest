package com.rag.ingestion.client.dto;

import java.util.List;

public record PageList(List<Page> results, Links _links) {}
