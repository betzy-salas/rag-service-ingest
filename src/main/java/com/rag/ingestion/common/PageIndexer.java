package com.rag.ingestion.common;

import com.rag.ingestion.common.mapper.DocumentMapper;
import com.rag.ingestion.common.model.ChunkMetadata;
import com.rag.ingestion.common.model.ConfluencePage;
import com.rag.ingestion.common.model.Section;
import com.rag.ingestion.mapper.ConfluencePageEntityMapper;
import com.rag.ingestion.repository.ConfluencePageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

/** Indexes Confluence pages: splits their text into chunks by section and stores their embeddings. */
@Component
@RequiredArgsConstructor
public class PageIndexer {

  // A whole page in one chunk scores below the similarity threshold for any specific question, so
  // sections longer than this are split further. The default (800) kept a full FAQ page in one chunk.
  private static final int CHUNK_SIZE_TOKENS = 300;

  private final VectorStore vectorStore;
  private final ConfluencePageRepository pageRepository;
  private final ConfluencePageEntityMapper pageEntityMapper;
  private final DocumentMapper documentMapper;
  private final HtmlSectionSplitter sectionSplitter;
  private final TokenTextSplitter splitter =
      TokenTextSplitter.builder().withChunkSize(CHUNK_SIZE_TOKENS).build();

  /**
   * Replaces the chunks of a page with the chunks of its current content and records the page.
   *
   * <p>Previous chunks are deleted first, so a page never mixes chunks of two versions. Each chunk
   * starts with the page title and the section heading, so the embedding carries the topic even
   * when the section text does not repeat it.
   *
   * @return the number of chunks created; {@code 0} if the page has no text
   */
  public int index(String spaceKey, ConfluencePage page) {
    vectorStore.delete(new FilterExpressionBuilder().eq(ChunkMetadata.PAGE_ID, page.id()).build());
    List<Document> sections =
        sectionSplitter.split(page.body()).stream()
            .map(section -> documentMapper.toDocument(page, spaceKey, withTitle(page.title(), section)))
            .toList();
    List<Document> chunks = splitter.apply(sections);
    if (!chunks.isEmpty()) {
      vectorStore.add(chunks);
    }
    pageRepository.save(pageEntityMapper.toEntity(page, spaceKey, chunks.size()));
    return chunks.size();
  }

  private static String withTitle(String title, Section section) {
    String header = section.heading().isEmpty() ? title : title + " > " + section.heading();
    return header + "\n" + section.text();
  }
}
