package com.rag.ingestion.common.mapper;

import com.rag.ingestion.common.model.ChunkMetadata;
import com.rag.ingestion.common.model.ConfluencePage;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.springframework.ai.document.Document;

/**
 * Builds the Spring AI {@link Document} of a page with the metadata expected by the query service.
 *
 * <p>Written by hand because MapStruct cannot populate the metadata map of a {@link Document}.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DocumentMapper {

  default Document toDocument(ConfluencePage page, String spaceKey, String text) {
    return new Document(
        text,
        Map.of(
            ChunkMetadata.PAGE_ID, page.id(),
            ChunkMetadata.SPACE_KEY, spaceKey,
            ChunkMetadata.TITLE, page.title(),
            ChunkMetadata.URL, page.url()));
  }
}
