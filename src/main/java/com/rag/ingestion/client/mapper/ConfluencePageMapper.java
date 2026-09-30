package com.rag.ingestion.client.mapper;

import com.rag.ingestion.client.dto.Page;
import com.rag.ingestion.common.model.ConfluencePage;
import com.rag.ingestion.common.model.ConfluencePageVersion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/** Maps Confluence API responses to the service model. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ConfluencePageMapper {

  /**
   * Maps a page with body.
   *
   * @param base wiki base URL (for example {@code https://site.atlassian.net/wiki}), prepended to
   *     the page link
   */
  @Mapping(target = "version", source = "page.version.number")
  @Mapping(target = "body", source = "page.body.storage.value")
  @Mapping(target = "url", expression = "java(base + page._links().webui())")
  ConfluencePage toConfluencePage(Page page, String base);

  @Mapping(target = "version", source = "version.number")
  ConfluencePageVersion toPageVersion(Page page);
}
