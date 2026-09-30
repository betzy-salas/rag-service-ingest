package com.rag.ingestion.service;

import com.rag.ingestion.client.ConfluenceClient;
import com.rag.ingestion.common.PageIndexer;
import com.rag.ingestion.common.model.ConfluencePageVersion;
import com.rag.ingestion.entity.ConfluencePageEntity;
import com.rag.ingestion.repository.ConfluencePageRepository;
import com.rag.ingestion.service.mapper.SummaryMapper;
import com.rag.ingestion.service.model.PageChange;
import com.rag.ingestion.service.model.SynchronizationSummary;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Synchronization of a space: indexes only new pages and pages with a new version. */
@Service
@RequiredArgsConstructor
public class SynchronizationService {

  private final ConfluenceClient confluenceClient;
  private final ConfluencePageRepository pageRepository;
  private final PageIndexer pageIndexer;
  private final SummaryMapper summaryMapper;

  /**
   * Synchronizes a space. Unchanged pages are neither downloaded nor embedded again.
   *
   * <p>Pages deleted in Confluence are not removed.
   */
  public SynchronizationSummary synchronize(String spaceKey) {
    Map<String, Integer> storedVersions = storedVersions(spaceKey);
    Map<PageChange, List<ConfluencePageVersion>> pagesByChange =
        confluenceClient.listPageVersions(spaceKey).stream()
            .collect(Collectors.groupingBy(page -> PageChange.of(page, storedVersions)));

    int chunks =
        Stream.of(PageChange.NEW, PageChange.UPDATED)
            .flatMap(change -> pagesByChange.getOrDefault(change, List.of()).stream())
            .mapToInt(page -> pageIndexer.index(spaceKey, confluenceClient.getPage(page.id())))
            .sum();

    return summaryMapper.toSynchronizationSummary(pagesByChange, chunks);
  }

  private Map<String, Integer> storedVersions(String spaceKey) {
    return pageRepository.findBySpaceKey(spaceKey).stream()
        .collect(Collectors.toMap(ConfluencePageEntity::getPageId, ConfluencePageEntity::getVersion));
  }
}
