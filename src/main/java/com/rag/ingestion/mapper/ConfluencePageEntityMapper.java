package com.rag.ingestion.mapper;

import com.rag.ingestion.common.model.ConfluencePage;
import com.rag.ingestion.entity.ConfluencePageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/** Maps an indexed Confluence page to its {@code confluence_page} row. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ConfluencePageEntityMapper {

  @Mapping(target = "pageId", source = "page.id")
  @Mapping(target = "indexedAt", expression = "java(java.time.OffsetDateTime.now())")
  ConfluencePageEntity toEntity(ConfluencePage page, String spaceKey, int chunkCount);
}
