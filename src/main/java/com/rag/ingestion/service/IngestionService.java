package com.rag.ingestion.service;

import com.rag.ingestion.client.ConfluenceClient;
import com.rag.ingestion.common.PageIndexer;
import com.rag.ingestion.common.model.ConfluencePage;
import com.rag.ingestion.service.mapper.SummaryMapper;
import com.rag.ingestion.service.model.IngestionSummary;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Full ingestion of a space: indexes every page, whether it changed or not. */
@Service
@RequiredArgsConstructor
public class IngestionService {

  private final ConfluenceClient confluenceClient;
  private final PageIndexer pageIndexer;
  private final SummaryMapper summaryMapper;

  /** Indexes every page of the space, replacing their previous chunks. */
  public IngestionSummary ingest(String spaceKey) {
    List<ConfluencePage> pages = confluenceClient.listPages(spaceKey);
    int chunks = pages.stream().mapToInt(page -> pageIndexer.index(spaceKey, page)).sum();
    return summaryMapper.toIngestionSummary(pages, chunks);
  }
}
