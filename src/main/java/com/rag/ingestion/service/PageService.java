package com.rag.ingestion.service;

import com.rag.ingestion.client.ConfluenceClient;
import com.rag.ingestion.service.mapper.PageInfoMapper;
import com.rag.ingestion.service.model.PageInfo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Lists the pages of a Confluence space without downloading their content. */
@Service
@RequiredArgsConstructor
public class PageService {

  private final ConfluenceClient confluenceClient;
  private final PageInfoMapper pageInfoMapper;

  /** Returns the id, title and version of every page of the space. */
  public List<PageInfo> listPages(String spaceKey) {
    return pageInfoMapper.toPageInfos(confluenceClient.listPageVersions(spaceKey));
  }
}
