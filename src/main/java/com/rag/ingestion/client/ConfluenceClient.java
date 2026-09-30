package com.rag.ingestion.client;

import com.rag.ingestion.client.dto.Page;
import com.rag.ingestion.client.dto.PageList;
import com.rag.ingestion.client.dto.Space;
import com.rag.ingestion.client.dto.SpaceList;
import com.rag.ingestion.client.mapper.ConfluencePageMapper;
import com.rag.ingestion.common.model.ConfluencePage;
import com.rag.ingestion.common.model.ConfluencePageVersion;
import com.rag.ingestion.config.ConfluenceProperties;
import com.rag.ingestion.exception.SpaceNotFoundException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Client for the Confluence Cloud REST API v2. */
@Component
@RequiredArgsConstructor
public class ConfluenceClient {

  private final RestClient confluenceRestClient;
  private final ConfluencePageMapper pageMapper;
  private final ConfluenceProperties properties;

  /** Returns every page of the space with its body. */
  public List<ConfluencePage> listPages(String spaceKey) {
    return listPageResponses(spaceKey, true).stream()
        .flatMap(
            response ->
                response.results().stream()
                    .map(page -> pageMapper.toConfluencePage(page, response._links().base())))
        .toList();
  }

  /** Returns every page of the space without body. Faster than {@link #listPages(String)}. */
  public List<ConfluencePageVersion> listPageVersions(String spaceKey) {
    return listPageResponses(spaceKey, false).stream()
        .flatMap(response -> response.results().stream())
        .map(pageMapper::toPageVersion)
        .toList();
  }

  /** Returns a page with its body. */
  public ConfluencePage getPage(String pageId) {
    Page page =
        confluenceRestClient
            .get()
            .uri(ConfluenceApi.PAGE_WITH_BODY, pageId)
            .retrieve()
            .body(Page.class);
    return pageMapper.toConfluencePage(page, properties.wikiUrl());
  }

  /** Fetches every batch of the listing by following {@code _links.next}. */
  private List<PageList> listPageResponses(String spaceKey, boolean withBody) {
    List<PageList> responses = new ArrayList<>();
    String path = withBody ? ConfluenceApi.SPACE_PAGES_WITH_BODY : ConfluenceApi.SPACE_PAGES;
    String next = path.formatted(findSpaceId(spaceKey), ConfluenceApi.PAGE_LIMIT);
    while (next != null) {
      PageList response = confluenceRestClient.get().uri(next).retrieve().body(PageList.class);
      responses.add(response);
      next = response._links().next();
    }
    return responses;
  }

  private String findSpaceId(String spaceKey) {
    SpaceList spaces =
        confluenceRestClient.get().uri(ConfluenceApi.SPACES_BY_KEY, spaceKey).retrieve().body(SpaceList.class);
    return spaces.results().stream()
        .findFirst()
        .map(Space::id)
        .orElseThrow(() -> new SpaceNotFoundException(spaceKey));
  }
}
