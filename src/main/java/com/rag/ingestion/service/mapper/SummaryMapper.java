package com.rag.ingestion.service.mapper;

import com.rag.ingestion.common.model.ConfluencePage;
import com.rag.ingestion.common.model.ConfluencePageVersion;
import com.rag.ingestion.service.model.IngestionSummary;
import com.rag.ingestion.service.model.PageChange;
import com.rag.ingestion.service.model.SynchronizationSummary;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/** Builds the summaries returned by the ingestion and synchronization services. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, imports = PageChange.class)
public interface SummaryMapper {

  @Mapping(target = "pages", expression = "java(pages.size())")
  @Mapping(target = "chunks", source = "chunks")
  IngestionSummary toIngestionSummary(List<ConfluencePage> pages, int chunks);

  @Mapping(target = "newPages", expression = "java(count(pagesByChange, PageChange.NEW))")
  @Mapping(target = "updatedPages", expression = "java(count(pagesByChange, PageChange.UPDATED))")
  @Mapping(target = "unchangedPages", expression = "java(count(pagesByChange, PageChange.UNCHANGED))")
  @Mapping(target = "chunks", source = "chunks")
  SynchronizationSummary toSynchronizationSummary(
      Map<PageChange, List<ConfluencePageVersion>> pagesByChange, int chunks);

  default int count(Map<PageChange, List<ConfluencePageVersion>> pagesByChange, PageChange change) {
    return pagesByChange.getOrDefault(change, List.of()).size();
  }
}
